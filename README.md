# FirstClub Membership Platform

A Java 21 and Spring Boot membership service for configurable tiers, plans, typed perks,
subscriptions, payments, refunds, criteria-based grants, and scheduled lifecycle maintenance.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop, or Docker Engine with Docker Compose

Docker is required locally because PostgreSQL stores the membership data and Redis provides the
per-user checkout lock.

### macOS Docker setup

If Docker Desktop is not already installed:

```bash
brew install --cask docker
open -a Docker
```

Wait for Docker Desktop to show that it is running, then verify the CLI:

```bash
docker version
docker compose version
```

## Run locally

From the repository root, start PostgreSQL and Redis first:

```bash
cd /Users/shambhavtyagi/Documents/firstclub-assignment
cp .env.example .env
docker compose up -d postgres redis
docker compose ps
```

The default ports are PostgreSQL `5432` and Redis `6379`. `.env` is optional when the defaults are
suitable, but is useful for overriding ports and database credentials.

Verify Redis before starting the application:

```bash
docker compose exec redis redis-cli ping
```

Expected output:

```text
PONG
```

Create clean API and jobs JARs:

```bash
mvn clean package -DskipTests
```

Start the API service:

```bash
java -jar membership-service/target/membership-service-0.0.1-SNAPSHOT.jar
```

Start the scheduler in a separate terminal:

```bash
java -jar membership-jobs/target/membership-jobs-0.0.1-SNAPSHOT.jar
```

`membership-service` is horizontally scalable and does not schedule work. Deploy
`membership-jobs` as exactly one replica; it shares the same PostgreSQL and Redis configuration,
owns no HTTP API, and runs the scheduled maintenance and monthly criteria work. Database migrations
remain the API service's startup responsibility, so start or migrate the API before the jobs process.

Run the fast local suite before packaging:

```bash
mvn -q test
```

Mockito's dynamic-agent warning on Java 23 and the intentional logged exception from
`MembershipCriteriaEvaluationJobTest` are expected; Maven must still exit with status `0`.

Docker-backed integration tests are intentionally not part of this repository's Maven suite.
Validate the locally running service through Swagger or the manual lifecycle flow below instead.

The service runs at `http://localhost:8080`. Flyway automatically creates or validates the
PostgreSQL schema. The base schema is created by
[`V1__create_membership_schema.sql`](membership-service/src/main/resources/db/migration/V1__create_membership_schema.sql),
including the indexes for batch reads, payment lookup, refunds, and scheduled maintenance.

This repository intentionally contains one consolidated **fresh-install** migration. If a
disposable local database has a stale Flyway history or an incomplete schema, recreate that local
database before restarting the API. Do not run this against a database containing data to keep:

```bash
docker compose down -v
docker compose up -d postgres redis
```

Stop local infrastructure with:

```bash
docker compose down
```

To deliberately remove all local PostgreSQL data too:

```bash
docker compose down -v
```

## API documentation

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI document: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

The APIs and Swagger UI are intentionally public for this assignment. Authentication and
authorization are not implemented yet.

Validation failures use a `400 VALIDATION_FAILED` response with a populated
`fieldViolations` array. Constraint conflicts raised by the application return `409 CONFLICT`;
unexpected database constraint conflicts return `409 DATA_INTEGRITY_VIOLATION` rather than being
misreported as an idempotency conflict.

## Verified local flow

The complete membership lifecycle was replayed against the Compose PostgreSQL and Redis
containers with the locally running service. The replay covered:

- clean PostgreSQL and Redis state, then Gold and Platinum tiers with monthly and quarterly plans;
- typed delivery, cart-discount, sale-access, deal-access, and support perks, including mapping,
  remapping, and archival/replacement;
- a duplicate perk in a single tier-creation request, verified to return `409` with all inserts
  rolled back;
- concurrent subscriptions for one user, verified to create only one pending payment through the
  Redis checkout lock; then payment failure, retry, and completion;
- Gold-to-Platinum quarterly upgrade, failed and retried payment, activation on successful payment,
  followed by Platinum-to-Gold quarterly downgrade with prorated refund allocations and simulated
  allocation completion;
- current-membership, history, available-catalog, and typed active-perk reads after each relevant
  transition; and
- enrollment availability at both levels: an enrollment-closed Silver tier was absent from the
  public catalog and rejected direct checkout with `409`; once reopened, an enrollment-closed
  quarterly plan was likewise hidden and rejected, while Silver's remaining open monthly plan
  created a pending checkout that was failed through the test callback; and
- criteria evaluation with nested boolean conditions, plus scheduled-maintenance handling for
  expired pending payments.

`mvn clean install` runs the automated suite. The opt-in client smoke test below calls the locally
running service, while the lifecycle replay above is an end-to-end manual verification using the
assignment testing callback endpoints.

## Project structure

```text
membership-models     API DTOs, enums, typed perk applications, response models
membership-commons    shared contracts, exceptions, RedisLock, UserCheckoutLock, local service stubs
membership-client     reusable asynchronous WebClient client for service consumers
membership-data       JPA entities, DAOs, repository interfaces and implementations
membership-core       business use cases: subscriptions, plan changes, payments, refunds, criteria
membership-jobs       standalone Spring Boot scheduler process
membership-service    standalone Spring Boot API process, HTTP controllers, Flyway, request logging
```

The dependency direction is intentionally one-way:

```text
service → core → data, commons, models
jobs → core → data, commons, models
client → models
```

`membership-service` and `membership-jobs` are independent executable modules. This permits the
API to scale with request volume while jobs remain one replica and do not duplicate scheduled work.

### Calling the service from another microservice

`membership-client` provides the direct WebClient-backed `UserMembershipClient`. It covers the public user catalog, current/history, active perks, subscription, retry,
cancellation, plan-change, and criteria-evaluation APIs. Every operation returns a
`CompletableFuture`, allowing a caller to compose asynchronous work or await a result with
`join()`/`get()`.

For an active membership's typed operational perks, call:

```text
GET /api/v1/users/{userId}/memberships/perks?appliesTo=DELIVERY&appliesTo=CART
```

`appliesTo` is optional and repeatable. Omitting it returns all active perks for the user's active
membership; a user without an active membership receives an empty list. The response includes the
typed perk application, for example delivery thresholds or cart-discount verticals.

```java
UserMembershipClient membershipClient =
    new UserMembershipClient("http://membership-service:8080");

MembershipSubscriptionResponse currentMembership =
    membershipClient.getCurrentMembership(userId).join();

List<UserMembershipPerkResponse> deliveryAndCartPerks =
    membershipClient
        .getActiveMembershipPerks(userId, List.of(PerkAppliesTo.DELIVERY, PerkAppliesTo.CART))
        .join();
```

The caller may instead pass a configured `WebClient` to add its own authentication, tracing,
timeouts, and connection-pool settings. Assignment-only `/api/v1/testing/**` endpoints are
intentionally not part of this reusable client.

The opt-in live client smoke test uses `http://localhost:8080` and can be run after starting the
service:

```bash
mvn -pl membership-client test -Dmembership.client.live.enabled=true
```

## Main classes and responsibilities

### Application and HTTP layer

- `MembershipServiceApplication` — Spring Boot API entry point and Flyway migration host.
- `AdminMembershipWriteController` — admin commands for tiers, plans, perks, mappings, and
  criteria.
- `AdminMembershipQueryController` — admin read APIs for tier, plan, and perk management.
- `UserMembershipWriteController` — user subscription, payment retry, plan-change, cancellation,
  and criteria-evaluation commands.
- `UserMembershipQueryController` — current membership, history, available-catalog, and active
  perk reads.

Controllers validate request DTOs, log the request actor, and pass `requestedBy` directly to the
write command that needs it. There is no request-thread audit context.

### Core membership use cases

- `UserMembershipCheckoutManager` — creates subscriptions, starts payments, completes/fails/retries
  payments, and activates a successful purchase.
- `MembershipPlanChangeManager` — calculates credits/refunds and creates a pending replacement
  subscription for upgrades or downgrades.
- `MembershipCancellationManager` — quotes and executes cancellation, then creates a refund and
  its payment-level refund allocations when money is due.
- `MembershipCriteriaEvaluationManager` — evaluates the recursive criteria tree, respects paid
  memberships, selects the highest-ranked matching tier, and grants free memberships.
- `MembershipMaintenanceManager` — expires active memberships, starts eligible auto-renewal
  attempts, and cancels expired pending checkouts.
- `AdminMembershipQueryManager`, `UserMembershipCatalogManager`, and
  `UserMembershipQueryManager` — orchestrate multi-entity query use cases without directly
  accessing repositories.
- focused `*FetchService` classes — each owns one repository for reusable reads of tiers, plans,
  perks, criteria, mappings, subscriptions, payments, or refunds.
- `UserPerksFetchHelper` — composes those focused reads to return typed perks unlocked by a
  user's active membership.
- `UserMembershipResponseHelper` — composes focused reads into membership responses.
- `TestingRefundSettlementService` — assignment-only stand-in for refund-gateway callbacks; it
  completes or fails individual refund allocations and derives the aggregate refund status.
- `MembershipTierWriteService`, `MembershipPlanWriteService`, `MembershipPerkWriteService`, and
  `MembershipTierCriteriaWriteService` — admin configuration commands.

All checkout-mutating paths use `RedisLock`; `UserCheckoutLock` is its Redis-backed implementation
and serializes work for one user while allowing different users to proceed independently. Each
public write-manager workflow owns an outer `@Transactional` boundary. Focused collaborators use
Spring's default `REQUIRED` propagation, so a checkout, plan change, cancellation, criteria grant,
or maintenance transition commits atomically or rolls back entirely.

### Persistence and data model

`membership-data` separates domain-facing repository interfaces from JPA details:

```text
core manager/fetch service → repository interface → repository implementation → DAO → PostgreSQL
```

The principal audited entities are `MembershipTierEntity`, `MembershipPlanEntity`,
`MembershipPerkEntity`, `MembershipTierCriteriaEntity`, `MembershipSubscriptionEntity`,
`PaymentEntity`, `RefundEntity`, and `RefundAllocationEntity`. They retain `createdBy` and
`updatedBy` for audit history.

### Explicit audit actors

Audit columns are ordinary JPA fields. `AuditableEntity.recordCreation(actor)` sets both
`createdBy` and `updatedBy`; `recordUpdate(actor)` changes only `updatedBy`. Command flows call
those methods explicitly after creating or mutating an entity. API writes use the request's
`requestedBy`, and scheduled maintenance and criteria jobs use `SYSTEM`.

This deliberately avoids `AuditorAware`, `@EnableJpaAuditing`, entity listeners, and `ThreadLocal`
state, so audit attribution remains correct when work is scheduled or otherwise leaves the HTTP
request thread. Audit fields remain excluded from API responses.

### Refund calculation and settlement

`ProratedRefundCalculator` calculates a cancellation or plan-change refund from the unused whole
calendar days of the membership term:

```text
refund = paid membership value × remaining days / total plan days
```

It rounds to two decimal places using `HALF_UP`. `RefundTransactionIdentificationService` then
allocates that refund against the original successful payments, largest payment first. The
resulting `RefundEntity` is the aggregate record, while `RefundAllocationEntity` records each
gateway-level payout that must be settled.

For this assignment, the following public test endpoints simulate payment-gateway and
refund-gateway callbacks:

```text
POST /api/v1/testing/payments/{paymentId}/complete
POST /api/v1/testing/payments/{paymentId}/fail
POST /api/v1/testing/refund-allocations/{allocationId}/complete
POST /api/v1/testing/refund-allocations/{allocationId}/fail
```

Completing every allocation marks the parent refund `COMPLETED`; failing one marks it `FAILED`.
These endpoints exist only to demonstrate the lifecycle before a real payment gateway is wired in.

Typed perk applications replace unstructured perk JSON:

- `DeliveryPerkApplication`
- `CartDiscountPerkApplication`
- `EarlyAccessSaleApplication`
- `MembershipDealApplication`
- `SupportPerkApplication`

## Design choices and invariants

| Choice | Reason |
| --- | --- |
| PostgreSQL for membership state | Subscriptions, payments, refunds, and their transitions need durable transactions, relational constraints, and queryable history. Typed perk and criteria payloads are stored as JSONB only where their shape is intentionally extensible. |
| Redis for the user checkout lock | A short distributed lock serializes subscription, retry, plan-change, cancellation, and criteria work for one user without serializing other users. PostgreSQL partial unique indexes remain the final correctness backstop. |
| Flyway-managed schema | Schema changes are versioned and validated at API startup; Hibernate validates rather than alters the production schema. |
| Case-insensitive unique tier names | `Gold`, `gold`, and `GOLD` must not become distinct customer-facing tiers. PostgreSQL enforces this with a unique `LOWER(display_name)` index. |
| Unique active perk display text per application area | An active `DELIVERY` perk cannot share the same normalized display-text key with another active delivery perk. The same text may exist for a different `appliesTo`, and an archived configuration can be recreated. An unassigned archived perk can instead be reactivated only within 24 hours of archival, provided no conflicting active perk exists. |
| One plan duration per tier | A tier cannot offer two 90-day plans, avoiding ambiguous renewal and plan-selection behavior. Price and presentation can be updated on that single plan. |
| Enrollment closure is enforced at tier and plan levels | A closed tier is omitted from the public catalog and cannot be directly subscribed to. An open tier can still selectively hide and reject an individual closed plan. Existing subscriptions are preserved; this is an acquisition gate, not a cancellation mechanism. |
| Idempotent tier-perk mappings | The tier/perk pair is unique. Repeating an assignment is safe and cannot expose the same perk twice. |
| Structured display text with `bold` | Text remains presentation data rather than HTML. Ordered segments plus a `bold` flag let mobile/web clients render emphasis consistently and safely, while `displayTextKey` supplies a normalized uniqueness key. |
| Instant-based persisted lifecycle timestamps | Activation, expiry, payment expiry, and audit timestamps are stored as UTC instants. Jobs use Asia/Kolkata only for the business schedule/cron boundary. |
| Paid memberships are never overwritten by criteria | A purchased membership has payment, credit, and refund implications. Criteria evaluation reports `PAID_MEMBERSHIP_ACTIVE` instead of silently changing its tier. |
| Criteria chooses the highest-priority matching configuration | Priorities are positive and unique. A higher-priority match replaces an existing criteria-granted membership with a new zero-cost, non-renewing grant; a regression test covers Gold-to-Platinum replacement. |
| Paid tier changes use payment/refund flows | Upgrades and downgrades create the appropriate pending payment or membership credit/refund allocation records. They do not mutate the active paid subscription in place. |
| Explicit audit actors | `createdBy` and `updatedBy` are stored on every principal membership table but excluded from public response DTOs. Commands call `recordCreation`/`recordUpdate` with `requestedBy`; job-originated changes explicitly use `SYSTEM`. |

These constraints are deliberately duplicated at the application and database levels where a race or
another deployment process could otherwise violate them.

### Scheduled work

- `MembershipMaintenanceJobs` in the standalone jobs process runs membership maintenance every 60
  seconds by default.
- `MembershipCriteriaEvaluationJob` in that process evaluates eligible users on the first day of every month at
  02:00 Asia/Kolkata by default.

Configuration can be overridden with:

```bash
MEMBERSHIP_MAINTENANCE_DELAY_MS=60000
MEMBERSHIP_PAYMENT_EXPIRY=PT15M
MEMBERSHIP_AUTO_RENEW_LEAD_TIME=PT24H
MEMBERSHIP_CRITERIA_CRON='0 0 2 1 * *'
```

Maintenance cancels a pending checkout when its latest direct-payment attempt has expired. A
pending direct payment becomes `VOIDED`. For a plan-change checkout, an expired `FAILED` direct
attempt also abandons the replacement: the failed gateway record remains `FAILED`, its successful
membership-credit payment becomes `VOIDED`, and the original active membership remains unchanged.

### Perk reassignment and archival

Perks are immutable configurations that can be shared by multiple tiers. Moving a perk between
tiers should preserve that configuration and its active status:

1. Add the existing perk to the destination tier.
2. Remove the mapping from the previous tier.

When the first step happens first, the perk is continuously mapped to at least one tier and remains
`ACTIVE`. Removing the final mapping archives the now-unassigned perk automatically; this keeps
the active catalog free of configuration that is no longer used.

If an operator removes the final mapping before adding the replacement mapping, the status endpoint
allows that archived, unassigned perk to be reactivated for 24 hours from its archival time. This is
a bounded recovery path for an ordering mistake, not an edit path: its display text and typed perk
application remain unchanged. After 24 hours, create a new perk configuration instead. Repeated
archive requests are idempotent and do not extend the recovery window.

## Next work

1. Add authentication and authorization, including role-based admin access and a trusted source
   for `requestedBy`.
2. Replace local `UserServiceInterface` and `OrderServiceInterface` stubs with resilient live
   integrations to the corresponding microservices.
3. Integrate a payment gateway, including payment initiation, webhook signature verification,
   idempotent callbacks, and real refund execution.
4. Restrict `/api/v1/testing/**` and Swagger/OpenAPI to local or non-production environments.
5. Add production observability: structured audit events, metrics, tracing, alerts, and dashboards
   for payments and jobs.
6. Add distributed job coordination before running multiple service replicas, so scheduled work is
   executed once per schedule rather than once per replica.
7. Expand automated integration coverage with PostgreSQL and Redis containers, particularly for
   payment expiry, locking, retries, refunds, and criteria scheduling.
8. Add deployment configuration, secret management, PostgreSQL backup/restore procedures, and a
   migration rollout strategy.
9. Add cache support for perks by tier ID and the active tier ID by user ID, with invalidation on
   tier-perk mapping and membership lifecycle changes.
