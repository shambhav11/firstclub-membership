package com.firstclub.membership.service.controller;

import com.firstclub.membership.core.command.*;
import com.firstclub.membership.models.request.admin.*;
import com.firstclub.membership.models.response.admin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
@Tag(
    name = "Admin membership writes",
    description = "Tier, plan, perk, and criteria configuration APIs.")
public class AdminMembershipWriteController {
  private static final Logger log = LoggerFactory.getLogger(AdminMembershipWriteController.class);
  private final MembershipWriteService membershipWriteService;
  private final TierPerkMappingWriteService tierPerkMappingWriteService;
  private final MembershipTierWriteService membershipTierWriteService;
  private final MembershipPlanWriteService membershipPlanWriteService;
  private final MembershipPerkWriteService membershipPerkWriteService;
  private final MembershipTierCriteriaWriteService membershipTierCriteriaWriteService;

  public AdminMembershipWriteController(
      MembershipWriteService membershipWriteService,
      TierPerkMappingWriteService tierPerkMappingWriteService,
      MembershipTierWriteService membershipTierWriteService,
      MembershipPlanWriteService membershipPlanWriteService,
      MembershipPerkWriteService membershipPerkWriteService,
      MembershipTierCriteriaWriteService membershipTierCriteriaWriteService) {
    this.membershipWriteService = membershipWriteService;
    this.tierPerkMappingWriteService = tierPerkMappingWriteService;
    this.membershipTierWriteService = membershipTierWriteService;
    this.membershipPlanWriteService = membershipPlanWriteService;
    this.membershipPerkWriteService = membershipPerkWriteService;
    this.membershipTierCriteriaWriteService = membershipTierCriteriaWriteService;
  }

  @PostMapping("/membership-tiers")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a tier with its initial plans and optional perks",
      description =
          "`displayName` is case-insensitively unique. A tier cannot contain duplicate plan"
              + " durations, and an active perk cannot duplicate the normalized display text for"
              + " the same `appliesTo`. `requestedBy` is stored internally for audit history.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Tier created."),
    @ApiResponse(
        responseCode = "400",
        description = "A required field or typed perk application is invalid.",
        content =
            @Content(
                schema =
                    @Schema(
                        implementation =
                            com.firstclub.membership.models.common.ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "Tier name, plan duration, or active perk uniqueness conflicts with configuration.",
        content =
            @Content(
                schema =
                    @Schema(
                        implementation =
                            com.firstclub.membership.models.common.ApiErrorResponse.class)))
  })
  public MembershipTierResponse createTier(
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "A sellable Gold tier with one monthly plan and delivery benefit.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"displayName":"Gold","enrollmentClosed":false,"initialPlans":[{"durationDays":30,"displayText":"Monthly","amount":500,"currencyCode":"INR","enrollmentClosed":false}],"perks":[{"displayText":[{"text":"Free delivery over 1000","bold":true}],"appliesTo":"DELIVERY","perkApplication":{"applicationType":"DELIVERY","discountPercent":100,"minOrderValue":1000,"currencyCode":"INR"}}],"requestedBy":"admin@example.com"}
""")))
          CreateMembershipTierRequest request) {
    log.info(
        "Received membership tier creation requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipWriteService.createTier(request);
  }

  @PostMapping("/membership-tiers/{tierId}/perks")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Assign existing perks to a tier",
      description =
          "Assignments are idempotent: repeating the same tier/perk pair succeeds without creating"
              + " a duplicate. Fails when the tier or a perk does not exist, or when a perk is"
              + " archived.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Perks assigned."),
    @ApiResponse(responseCode = "400", description = "The perk id list or requestedBy is missing."),
    @ApiResponse(responseCode = "404", description = "Tier or requested perk was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "A requested perk is archived or cannot be assigned.")
  })
  public void addPerks(
      @PathVariable UUID tierId, @Valid @RequestBody AddPerksToTierRequest request) {
    log.info(
        "Received tier perk assignment requestedBy={} request={}", request.requestedBy(), request);
      tierPerkMappingWriteService.add(tierId, request.membershipPerkIds(), request.requestedBy());
  }

  @DeleteMapping("/membership-tiers/{tierId}/perks")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Remove perks from a tier",
      description =
          "Removes the supplied tier/perk mappings. The perks remain available in the reusable perk"
              + " library and can be assigned to another tier later. `requestedBy` is retained for"
              + " audit history.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Perks removed from the tier."),
    @ApiResponse(responseCode = "400", description = "The perk id list or requestedBy is missing."),
    @ApiResponse(responseCode = "404", description = "The tier or requested perk was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "A supplied perk is not currently assigned to this tier.")
  })
  public void removePerks(
      @PathVariable UUID tierId,
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Existing perk mappings to remove from the tier.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"membershipPerkIds":["22222222-2222-4222-8222-222222222222"],"requestedBy":"admin@example.com"}
""")))
          RemovePerksFromTierRequest request) {
    log.info(
        "Received tier perk removal requestedBy={} request={}", request.requestedBy(), request);
      tierPerkMappingWriteService.remove(tierId, request.membershipPerkIds(), request.requestedBy());
  }

  @PutMapping("/membership-tiers/{tierId}")
  @Operation(
      summary = "Update a tier",
      description =
          "Updates the tier name, lifecycle status, and enrollment availability. A display name is"
              + " case-insensitively unique. Closing enrollment hides the tier from new user"
              + " selections without changing existing memberships.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tier updated."),
    @ApiResponse(responseCode = "400", description = "A required field or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "The tier was not found."),
    @ApiResponse(responseCode = "409", description = "Another tier already uses this display name.")
  })
  public MembershipTierSummaryResponse updateTier(
      @PathVariable UUID tierId,
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "The desired active, enrollment-open Gold tier configuration.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"displayName":"Gold","status":"ACTIVE","enrollmentClosed":false,"requestedBy":"admin@example.com"}
""")))
          UpdateMembershipTierRequest request) {
    log.info(
        "Received membership tier update requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipTierWriteService.update(tierId, request);
  }

  @PutMapping("/membership-tiers/{tierId}/criteria")
  @Operation(
      summary = "Create or replace tier eligibility criteria",
      description =
          "Stores one nested boolean expression, evaluation window, priority, and grant plan for"
              + " the tier; `grantedPlanId` must belong to that tier. A criteria-granted"
              + " membership is always zero-cost and non-renewing, regardless of the plan's catalog"
              + " price. Groups use `AND` or `OR` and contain only `children`. `ORDER_COUNT` and"
              + " `ORDER_VALUE` conditions use `GREATER_THAN` or `LESS_THAN` with a non-negative"
              + " numeric threshold (`ORDER_COUNT` must be an integer); to express at least 1000"
              + " orders, use `GREATER_THAN` with `999`. `COHORT` conditions use `IN_ANY` or"
              + " `IN_ALL` with a non-empty string-array threshold. When several criteria match,"
              + " the lowest priority number wins. Paid memberships are not moved by criteria"
              + " evaluation.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Criteria created or replaced."),
    @ApiResponse(
        responseCode = "400",
        description = "The priority, grant plan, expression, or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "The tier or grant plan was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "The selected grant plan does not belong to the tier.")
  })
  public MembershipTierCriteriaResponse upsertTierCriteria(
      @PathVariable UUID tierId,
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Monthly eligibility: order count of at least 1000.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"priority":1,"evaluationWindow":"MONTHLY","grantedPlanId":"33333333-3333-4333-8333-333333333333","expression":{"parameter":"ORDER_COUNT","operator":"GREATER_THAN","threshold":999},"requestedBy":"admin@example.com"}
""")))
          UpsertMembershipTierCriteriaRequest request) {
    log.info(
        "Received tier criteria update requestedBy={} request={}", request.requestedBy(), request);
      return membershipTierCriteriaWriteService.upsert(tierId, request);
  }

  @PostMapping("/membership-plans")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a plan for a tier",
      description =
          "A tier has at most one plan for each `durationDays` value. Only ACTIVE, enrollment-open"
              + " plans appear in the user catalog.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Plan created."),
    @ApiResponse(
        responseCode = "400",
        description = "Amount, currency, duration, or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "The membership tier was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "The tier already has a plan with this duration.")
  })
  public MembershipPlanResponse createPlan(
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "A 90-day Gold plan.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"membershipTierId":"11111111-1111-4111-8111-111111111111","durationDays":90,"displayText":"Quarterly","amount":1000,"currencyCode":"INR","enrollmentClosed":false,"requestedBy":"admin@example.com"}
""")))
          CreateMembershipPlanRequest request) {
    log.info(
        "Received membership plan creation requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipPlanWriteService.create(request);
  }

  @PutMapping("/membership-plans/{planId}")
  @Operation(
      summary = "Update a membership plan",
      description =
          "Updates the sellable plan configuration. `enrollmentClosed` hides a plan from new"
              + " selections while preserving existing subscriptions. Amount is zero only for a"
              + " criteria-granted plan.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Plan updated."),
    @ApiResponse(
        responseCode = "400",
        description = "Amount, currency, status, or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "The plan was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "The requested update conflicts with plan invariants.")
  })
  public MembershipPlanResponse updatePlan(
      @PathVariable UUID planId,
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "An active, enrollment-open quarterly Gold plan.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"displayText":"Quarterly","amount":1000,"currencyCode":"INR","status":"ACTIVE","enrollmentClosed":false,"requestedBy":"admin@example.com"}
""")))
          UpdateMembershipPlanRequest request) {
    log.info(
        "Received membership plan update requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipPlanWriteService.update(planId, request);
  }

  @PostMapping("/membership-perks")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a reusable typed perk",
      description =
          "`perkApplication.applicationType` must match `appliesTo`. Use `DELIVERY` with"
              + " `appliesTo: DELIVERY` and `discountPercent` (greater than 0 through 100),"
              + " `minOrderValue` (zero or more), and a three-letter `currencyCode`;"
              + " `CART_DISCOUNT` with `appliesTo: CART` and"
              + " `discountPercentBasisPoints` (1 through 10000) plus non-empty"
              + " `eligibleVerticals`; `EARLY_ACCESS_SALE` with `appliesTo: SALE` and"
              + " `offsetValueInHours` (zero or more); `MEMBERSHIP_DEAL` with"
              + " `appliesTo: MEMBERSHIP_DEAL` and non-empty `applicableDealTiers`; or"
              + " `SUPPORT` with `appliesTo: SUPPORT`, `callbackWithinUnit` (`MINUTES` or"
              + " `HOURS`), and `callbackWithinValue` (at least 1). An active perk's normalized"
              + " display text must be unique within its application area.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Perk created."),
    @ApiResponse(
        responseCode = "400",
        description = "Display text or typed application fields are invalid."),
    @ApiResponse(
        responseCode = "409",
        description = "An active perk with this display text and appliesTo already exists.")
  })
  public MembershipPerkResponse createPerk(
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "A reusable cart-discount perk.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
                  {"displayText":[{"text":"5% off flour","bold":true}],"appliesTo":"CART","perkApplication":{"applicationType":"CART_DISCOUNT","discountPercentBasisPoints":500,"eligibleVerticals":["flour"]},"requestedBy":"admin@example.com"}
""")))
          CreateMembershipPerkRequest request) {
    log.info(
        "Received membership perk creation requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipPerkWriteService.create(request);
  }

  @PutMapping("/membership-perks/{perkId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Archive or reactivate a reusable perk",
      description =
          "Changes a perk's lifecycle status without deleting it. Archived perks cannot be newly"
              + " assigned; mapped perks do not disappear from a tier's historical configuration."
              + " An archived, unassigned perk may be reactivated only within 24 hours of"
              + " archival.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Perk status updated."),
    @ApiResponse(
        responseCode = "400",
        description = "Status or requestedBy is missing or invalid."),
    @ApiResponse(responseCode = "404", description = "The perk was not found."),
    @ApiResponse(
        responseCode = "409",
        description =
            "The perk is mapped, already active, older than 24 hours, or conflicts with an active"
                + " perk.")
  })
  public void updatePerkStatus(
      @PathVariable UUID perkId,
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description =
                  "Archive a perk, or reactivate an unassigned perk within 24 hours of archival.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"status":"ARCHIVED","requestedBy":"admin@example.com"}
""")))
          UpdateMembershipPerkRequest request) {
    log.info(
        "Received membership perk status update requestedBy={} request={}",
        request.requestedBy(),
        request);
      membershipPerkWriteService.updateStatus(perkId, request);
  }
}
