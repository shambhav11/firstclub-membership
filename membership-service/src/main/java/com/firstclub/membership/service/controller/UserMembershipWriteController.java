package com.firstclub.membership.service.controller;

import com.firstclub.membership.commons.OrderServiceInterface;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.command.MembershipCancellationManager;
import com.firstclub.membership.core.command.MembershipCriteriaEvaluationManager;
import com.firstclub.membership.core.command.MembershipPlanChangeManager;
import com.firstclub.membership.core.command.TestingRefundSettlementService;
import com.firstclub.membership.core.command.UserMembershipCheckoutManager;
import com.firstclub.membership.models.request.testing.CompletePaymentRequest;
import com.firstclub.membership.models.request.testing.FailPaymentRequest;
import com.firstclub.membership.models.request.testing.SetCriteriaFactsRequest;
import com.firstclub.membership.models.request.user.CancelMembershipRequest;
import com.firstclub.membership.models.request.user.ChangeMembershipPlanRequest;
import com.firstclub.membership.models.request.user.SubscribeMembershipRequest;
import com.firstclub.membership.models.response.user.CriteriaEvaluationResponse;
import com.firstclub.membership.models.response.user.MembershipCancellationQuoteResponse;
import com.firstclub.membership.models.response.user.MembershipChangeQuoteResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(
    name = "User membership writes",
    description = "Subscription, payment, cancellation, plan-change, and criteria APIs.")
public class UserMembershipWriteController {
  private static final Logger log = LoggerFactory.getLogger(UserMembershipWriteController.class);
  private final UserMembershipCheckoutManager userMembershipCheckoutManager;
  private final MembershipCancellationManager membershipCancellationManager;
  private final MembershipPlanChangeManager membershipPlanChangeManager;
  private final TestingRefundSettlementService testingRefundSettlementService;
  private final MembershipCriteriaEvaluationManager membershipCriteriaEvaluationManager;
  private final OrderServiceInterface orderServiceInterface;
  private final UserServiceInterface userServiceInterface;

  public UserMembershipWriteController(
      UserMembershipCheckoutManager userMembershipCheckoutManager,
      MembershipCancellationManager membershipCancellationManager,
      MembershipPlanChangeManager membershipPlanChangeManager,
      TestingRefundSettlementService testingRefundSettlementService,
      MembershipCriteriaEvaluationManager membershipCriteriaEvaluationManager,
      OrderServiceInterface orderServiceInterface,
      UserServiceInterface userServiceInterface) {
    this.userMembershipCheckoutManager = userMembershipCheckoutManager;
    this.membershipCancellationManager = membershipCancellationManager;
    this.membershipPlanChangeManager = membershipPlanChangeManager;
    this.testingRefundSettlementService = testingRefundSettlementService;
    this.membershipCriteriaEvaluationManager = membershipCriteriaEvaluationManager;
    this.orderServiceInterface = orderServiceInterface;
    this.userServiceInterface = userServiceInterface;
  }

  @PostMapping("/api/v1/users/{userId}/memberships/criteria/evaluate")
  @Operation(
      summary = "Evaluate criteria-based membership eligibility",
      description =
          "Selects the highest-priority matching criterion. It creates or replaces only a free"
              + " criteria-granted membership; an active purchased membership is returned unchanged"
              + " because payment and refund records are involved.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Evaluation outcome, including the matched tier when applicable."),
    @ApiResponse(responseCode = "400", description = "requestedBy is blank."),
    @ApiResponse(responseCode = "404", description = "The user does not exist."),
    @ApiResponse(
        responseCode = "409",
        description = "The user has a pending membership payment or invalid stored criteria.")
  })
  public CriteriaEvaluationResponse evaluateCriteria(
      @PathVariable UUID userId,
      @Parameter(
              description = "Internal audit actor, for example admin@example.com",
              example = "admin@example.com")
          @RequestParam
          @NotBlank
          String requestedBy) {
    log.info("Received criteria evaluation requestedBy={}", requestedBy);
      return membershipCriteriaEvaluationManager.evaluate(userId, requestedBy);
  }

  @PostMapping("/api/v1/testing/users/{userId}/criteria-facts")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "[Testing] Set criteria facts for a user",
      description =
          "Assignment-only user/order-service stand-in. Sets order count, order value, and cohorts"
              + " used by criteria evaluation.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Facts stored."),
    @ApiResponse(responseCode = "400", description = "Facts or requestedBy are invalid.")
  })
  public void setCriteriaFacts(
      @PathVariable UUID userId, @Valid @RequestBody SetCriteriaFactsRequest request) {
    log.info(
        "Received test criteria facts update requestedBy={} request={}",
        request.requestedBy(),
        request);
      orderServiceInterface.setFactsForTesting(userId, request.orderCount(), request.orderValue());
      userServiceInterface.setCohortsForTesting(userId, request.cohorts());
  }

  @PostMapping("/api/v1/users/{userId}/memberships/subscriptions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Start a paid membership checkout",
      description =
          "Creates one pending subscription and direct payment. Redis serializes checkout for the"
              + " user and a database constraint prevents a second pending subscription. Complete"
              + " or fail it with Swagger's `POST /api/v1/testing/payments/{paymentId}/complete`"
              + " or `/fail` callback.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Pending subscription and payment created."),
    @ApiResponse(
        responseCode = "400",
        description = "Plan id, payment method reference, or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "User or selected plan was not found."),
    @ApiResponse(
        responseCode = "409",
        description =
            "A pending checkout exists, or the plan/tier is not available for enrollment.")
  })
  public MembershipSubscriptionResponse subscribe(
      @PathVariable UUID userId,
      @Valid
          @RequestBody
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "The plan to purchase and whether it should automatically renew.",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
{"membershipPlanId":"11111111-1111-4111-8111-111111111111","autoRenew":false,"requestedBy":"user@example.com"}
""")))
          SubscribeMembershipRequest request) {
    log.info(
        "Received membership subscription requestedBy={} request={}",
        request.requestedBy(),
        request);
      return userMembershipCheckoutManager.subscribe(userId, request);
  }

  @PostMapping("/api/v1/users/{userId}/memberships/subscriptions/{subscriptionId}/payments/retry")
  @Operation(
      summary = "Retry a failed membership payment",
      description =
          "Creates a new pending direct payment only after the previous payment has failed. A"
              + " pending payment is not retried in parallel. Mark the new attempt processed or"
              + " failed with Swagger's payment testing callback.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "New payment attempt created."),
    @ApiResponse(responseCode = "400", description = "requestedBy is blank."),
    @ApiResponse(responseCode = "404", description = "User or subscription was not found."),
    @ApiResponse(
        responseCode = "409",
        description =
            "The subscription is not eligible for retry or already has a pending payment.")
  })
  public MembershipSubscriptionResponse retryPayment(
      @PathVariable UUID userId,
      @PathVariable UUID subscriptionId,
      @RequestParam @NotBlank String requestedBy) {
    log.info("Received payment retry requestedBy={}", requestedBy);
      return userMembershipCheckoutManager.retryPayment(userId, subscriptionId, requestedBy);
  }

  @PostMapping("/api/v1/users/{userId}/memberships/current/cancellation-quote")
  @Operation(
      summary = "Quote cancellation and prorated refund",
      description =
          "Calculates the unused membership value before cancellation. It does not mutate the"
              + " subscription or start a refund.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cancellation and refund quote."),
    @ApiResponse(responseCode = "400", description = "requestedBy is blank."),
    @ApiResponse(responseCode = "404", description = "User or active membership was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "The active membership is not eligible for cancellation.")
  })
  public MembershipCancellationQuoteResponse cancellationQuote(
      @PathVariable UUID userId, @RequestParam @NotBlank String requestedBy) {
    log.info("Received membership cancellation quote requestedBy={}", requestedBy);
      return membershipCancellationManager.quote(userId);
  }

  @PostMapping("/api/v1/users/{userId}/memberships/current/cancel")
  @Operation(
      summary = "Cancel the active membership",
      description =
          "Cancels the active subscription and creates refund allocations against successful"
              + " original payments when money is due. Settle every allocation through Swagger's"
              + " `POST /api/v1/testing/refund-allocations/{allocationId}/complete` or `/fail`"
              + " callback.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Membership cancelled; any refund is included in the response."),
    @ApiResponse(responseCode = "400", description = "The cancellation request is invalid."),
    @ApiResponse(responseCode = "404", description = "User or active membership was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "Cancellation conflicts with the membership's current lifecycle state.")
  })
  public MembershipSubscriptionResponse cancel(
      @PathVariable UUID userId, @Valid @RequestBody CancelMembershipRequest request) {
    log.info(
        "Received membership cancellation requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipCancellationManager.cancel(userId, request);
  }

  @PostMapping("/api/v1/users/{userId}/memberships/plan-change/quote")
  @Operation(
      summary = "Quote an upgrade or downgrade",
      description =
          "Returns the credit from unused value, amount due, and any refund before changing"
              + " membership state. It has no side effects.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Plan-change quote."),
    @ApiResponse(responseCode = "400", description = "Target plan or requestedBy is invalid."),
    @ApiResponse(
        responseCode = "404",
        description = "User, active membership, or target plan was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "The plan is not eligible or a pending checkout blocks the change.")
  })
  public MembershipChangeQuoteResponse quotePlanChange(
      @PathVariable UUID userId, @Valid @RequestBody ChangeMembershipPlanRequest request) {
    log.info(
        "Received membership plan-change quote requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipPlanChangeManager.quote(userId, request);
  }

  @PostMapping("/api/v1/users/{userId}/memberships/plan-change")
  @Operation(
      summary = "Change the membership plan",
      description =
          "Uses the quoted credit/refund calculation. A payment-required change creates a pending"
              + " replacement; a zero-due downgrade activates the replacement and records any"
              + " refund allocation. For a pending replacement, mark its payment processed or"
              + " failed with Swagger's `POST /api/v1/testing/payments/{paymentId}/complete` or"
              + " `/fail` callback.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Replacement subscription created or activated."),
    @ApiResponse(responseCode = "400", description = "Target plan or requestedBy is invalid."),
    @ApiResponse(
        responseCode = "404",
        description = "User, active membership, or target plan was not found."),
    @ApiResponse(
        responseCode = "409",
        description = "A pending checkout exists or the requested transition is not allowed.")
  })
  public MembershipSubscriptionResponse changePlan(
      @PathVariable UUID userId, @Valid @RequestBody ChangeMembershipPlanRequest request) {
    log.info(
        "Received membership plan change requestedBy={} request={}",
        request.requestedBy(),
        request);
      return membershipPlanChangeManager.change(userId, request);
  }

  @PostMapping("/api/v1/testing/payments/{paymentId}/complete")
  @Operation(
      summary = "[Testing] Complete a pending payment",
      description =
          "Assignment-only payment-gateway callback simulator. Activates the pending subscription.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Payment completed and subscription activated."),
    @ApiResponse(responseCode = "400", description = "bankReferenceId or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "Payment was not found."),
    @ApiResponse(responseCode = "409", description = "Payment is no longer pending.")
  })
  public MembershipSubscriptionResponse completePayment(
      @PathVariable UUID paymentId, @Valid @RequestBody CompletePaymentRequest request) {
    log.info("Received test payment completion requestedBy={}", request.requestedBy());
      return userMembershipCheckoutManager.completePayment(paymentId, request.bankReferenceId(), request.requestedBy());
  }

  @PostMapping("/api/v1/testing/payments/{paymentId}/fail")
  @Operation(
      summary = "[Testing] Fail a pending payment",
      description =
          "Assignment-only payment-gateway callback simulator. The subscription can then be retried"
              + " or expire.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Payment failed; retry is now possible."),
    @ApiResponse(responseCode = "400", description = "Failure reason or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "Payment was not found."),
    @ApiResponse(responseCode = "409", description = "Payment is no longer pending.")
  })
  public MembershipSubscriptionResponse failPayment(
      @PathVariable UUID paymentId, @Valid @RequestBody FailPaymentRequest request) {
    log.info("Received test payment failure requestedBy={}", request.requestedBy());
      return userMembershipCheckoutManager.failPayment(paymentId, request.requestedBy());
  }

  @PostMapping("/api/v1/testing/refund-allocations/{allocationId}/complete")
  @Operation(
      summary = "[Testing] Complete a refund allocation",
      description =
          "Assignment-only refund-gateway callback simulator. The parent refund completes after"
              + " every allocation succeeds.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Allocation completed; parent refund status is recalculated."),
    @ApiResponse(responseCode = "400", description = "bankReferenceId or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "Refund allocation was not found."),
    @ApiResponse(responseCode = "409", description = "Allocation is no longer pending.")
  })
  public com.firstclub.membership.models.response.user.RefundSummaryResponse completeRefund(
      @PathVariable UUID allocationId, @Valid @RequestBody CompletePaymentRequest request) {
    log.info("Received test refund completion requestedBy={}", request.requestedBy());
      return testingRefundSettlementService.complete(allocationId, request.bankReferenceId(), request.requestedBy());
  }

  @PostMapping("/api/v1/testing/refund-allocations/{allocationId}/fail")
  @Operation(
      summary = "[Testing] Fail a refund allocation",
      description =
          "Assignment-only refund-gateway callback simulator. One failure marks the parent refund"
              + " failed.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Allocation and parent refund marked failed."),
    @ApiResponse(responseCode = "400", description = "Failure reason or requestedBy is invalid."),
    @ApiResponse(responseCode = "404", description = "Refund allocation was not found."),
    @ApiResponse(responseCode = "409", description = "Allocation is no longer pending.")
  })
  public com.firstclub.membership.models.response.user.RefundSummaryResponse failRefund(
      @PathVariable UUID allocationId, @Valid @RequestBody FailPaymentRequest request) {
    log.info("Received test refund failure requestedBy={}", request.requestedBy());
      return testingRefundSettlementService.fail(allocationId, request.reason(), request.requestedBy());
  }
}
