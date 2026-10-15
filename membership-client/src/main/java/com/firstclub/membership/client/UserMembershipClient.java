package com.firstclub.membership.client;

import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.PageResponse;
import com.firstclub.membership.models.request.user.CancelMembershipRequest;
import com.firstclub.membership.models.request.user.ChangeMembershipPlanRequest;
import com.firstclub.membership.models.request.user.SubscribeMembershipRequest;
import com.firstclub.membership.models.response.user.CriteriaEvaluationResponse;
import com.firstclub.membership.models.response.user.MembershipCancellationQuoteResponse;
import com.firstclub.membership.models.response.user.MembershipCatalogResponse;
import com.firstclub.membership.models.response.user.MembershipChangeQuoteResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import com.firstclub.membership.models.response.user.UserMembershipPerkResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/** Asynchronous WebClient client for user-facing Membership APIs. */
public final class UserMembershipClient {
  private static final ParameterizedTypeReference<PageResponse<MembershipSubscriptionResponse>>
      MEMBERSHIP_HISTORY_TYPE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<UserMembershipPerkResponse>>
      USER_PERKS_TYPE = new ParameterizedTypeReference<>() {};

  private final WebClient webClient;

  /** Creates a client targeting the supplied Membership service base URL. */
  public UserMembershipClient(String baseUrl) {
    this(WebClient.builder().baseUrl(Objects.requireNonNull(baseUrl, "baseUrl")).build());
  }

  /**
   * Creates a client using a caller-configured WebClient, for example one with auth and tracing.
   */
  public UserMembershipClient(WebClient webClient) {
    this.webClient = Objects.requireNonNull(webClient, "webClient");
  }

  public CompletableFuture<MembershipCatalogResponse> getMembershipOptions() {
    return webClient
        .get()
        .uri("/api/v1/membership-options")
        .retrieve()
        .bodyToMono(MembershipCatalogResponse.class)
        .toFuture();
  }

  public CompletableFuture<MembershipSubscriptionResponse> getCurrentMembership(UUID userId) {
    return webClient
        .get()
        .uri("/api/v1/users/{userId}/memberships/current", userId)
        .retrieve()
        .bodyToMono(MembershipSubscriptionResponse.class)
        .toFuture();
  }

  public CompletableFuture<PageResponse<MembershipSubscriptionResponse>> getMembershipHistory(
      UUID userId, int page, int size) {
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/v1/users/{userId}/memberships/history")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build(userId))
        .retrieve()
        .bodyToMono(MEMBERSHIP_HISTORY_TYPE)
        .toFuture();
  }

  /** Retrieves active membership perks, optionally restricted to the supplied applications. */
  public CompletableFuture<List<UserMembershipPerkResponse>> getActiveMembershipPerks(
      UUID userId, List<PerkAppliesTo> appliesTo) {
    return webClient
        .get()
        .uri(
            uriBuilder -> {
              var builder = uriBuilder.path("/api/v1/users/{userId}/memberships/perks");
              if (appliesTo != null && !appliesTo.isEmpty()) {
                builder.queryParam("appliesTo", appliesTo);
              }
              return builder.build(userId);
            })
        .retrieve()
        .bodyToMono(USER_PERKS_TYPE)
        .toFuture();
  }

  public CompletableFuture<MembershipSubscriptionResponse> subscribe(
      UUID userId, SubscribeMembershipRequest request) {
    return post("/api/v1/users/{userId}/memberships/subscriptions", userId, request);
  }

  public CompletableFuture<MembershipSubscriptionResponse> retryPayment(
      UUID userId, UUID subscriptionId, String requestedBy) {
    return webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(
                        "/api/v1/users/{userId}/memberships/subscriptions/{subscriptionId}/payments/retry")
                    .queryParam("requestedBy", requestedBy)
                    .build(userId, subscriptionId))
        .retrieve()
        .bodyToMono(MembershipSubscriptionResponse.class)
        .toFuture();
  }

  public CompletableFuture<MembershipCancellationQuoteResponse> getCancellationQuote(
      UUID userId, String requestedBy) {
    return webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/v1/users/{userId}/memberships/current/cancellation-quote")
                    .queryParam("requestedBy", requestedBy)
                    .build(userId))
        .retrieve()
        .bodyToMono(MembershipCancellationQuoteResponse.class)
        .toFuture();
  }

  public CompletableFuture<MembershipSubscriptionResponse> cancel(
      UUID userId, CancelMembershipRequest request) {
    return post("/api/v1/users/{userId}/memberships/current/cancel", userId, request);
  }

  public CompletableFuture<MembershipChangeQuoteResponse> getPlanChangeQuote(
      UUID userId, ChangeMembershipPlanRequest request) {
    return webClient
        .post()
        .uri("/api/v1/users/{userId}/memberships/plan-change/quote", userId)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(MembershipChangeQuoteResponse.class)
        .toFuture();
  }

  public CompletableFuture<MembershipSubscriptionResponse> changePlan(
      UUID userId, ChangeMembershipPlanRequest request) {
    return post("/api/v1/users/{userId}/memberships/plan-change", userId, request);
  }

  public CompletableFuture<CriteriaEvaluationResponse> evaluateCriteria(
      UUID userId, String requestedBy) {
    return webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/v1/users/{userId}/memberships/criteria/evaluate")
                    .queryParam("requestedBy", requestedBy)
                    .build(userId))
        .retrieve()
        .bodyToMono(CriteriaEvaluationResponse.class)
        .toFuture();
  }

  private CompletableFuture<MembershipSubscriptionResponse> post(
      String path, UUID userId, Object request) {
    return webClient
        .post()
        .uri(path, userId)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(MembershipSubscriptionResponse.class)
        .toFuture();
  }
}
