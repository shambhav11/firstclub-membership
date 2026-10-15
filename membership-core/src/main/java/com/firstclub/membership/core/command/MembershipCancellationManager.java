package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.query.UserMembershipQueryManager;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.RefundFetchService;
import com.firstclub.membership.core.refund.ProratedRefundCalculator;
import com.firstclub.membership.core.refund.RefundQuote;
import com.firstclub.membership.core.refund.RefundTransactionIdentificationService;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.RefundAllocationEntity;
import com.firstclub.membership.data.entity.RefundEntity;
import com.firstclub.membership.models.RefundStatus;
import com.firstclub.membership.models.SubscriptionOrigin;
import com.firstclub.membership.models.SubscriptionStatus;
import com.firstclub.membership.models.request.user.CancelMembershipRequest;
import com.firstclub.membership.models.response.user.MembershipCancellationQuoteResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipCancellationManager {
  private final UserServiceInterface userServiceInterface;
  private final UserCheckoutTransactionExecutor transactionExecutor;
  private final MembershipSubscriptionFetchService subscriptionFetchService;
  private final MembershipPlanFetchService planFetchService;
  private final RefundFetchService refundFetchService;
  private final RefundWriteService refundWriteService;
  private final RefundAllocationWriteService refundAllocationWriteService;
  private final ProratedRefundCalculator refundCalculator;
  private final UserMembershipQueryManager userMembershipQueryManager;
  private final RefundTransactionIdentificationService transactionIdentificationService;

  public MembershipCancellationManager(
      UserServiceInterface userServiceInterface,
      UserCheckoutTransactionExecutor transactionExecutor,
      MembershipSubscriptionFetchService subscriptionFetchService,
      MembershipPlanFetchService planFetchService,
      RefundFetchService refundFetchService,
      RefundWriteService refundWriteService,
      RefundAllocationWriteService refundAllocationWriteService,
      ProratedRefundCalculator refundCalculator,
      UserMembershipQueryManager userMembershipQueryManager,
      RefundTransactionIdentificationService transactionIdentificationService) {
    this.userServiceInterface = userServiceInterface;
    this.transactionExecutor = transactionExecutor;
    this.subscriptionFetchService = subscriptionFetchService;
    this.planFetchService = planFetchService;
    this.refundFetchService = refundFetchService;
    this.refundWriteService = refundWriteService;
    this.refundAllocationWriteService = refundAllocationWriteService;
    this.refundCalculator = refundCalculator;
    this.userMembershipQueryManager = userMembershipQueryManager;
    this.transactionIdentificationService = transactionIdentificationService;
  }

  @Transactional(readOnly = true)
  public MembershipCancellationQuoteResponse quote(UUID userId) {
    requireUser(userId);
    MembershipSubscriptionEntity subscription = requireActive(userId);
    var plan = requirePlan(subscription);
    RefundQuote refund = calculateRefund(subscription, plan.getDurationDays(), Instant.now());
    return new MembershipCancellationQuoteResponse(
        subscription.getId(),
        plan.getDurationDays(),
        refund.usedDays(),
        refund.remainingDays(),
        refund.amount(),
        subscription.getCurrencyCode());
  }

  public MembershipSubscriptionResponse cancel(UUID userId, CancelMembershipRequest request) {
    return transactionExecutor.execute(userId, () -> cancelLocked(userId, request));
  }

  private MembershipSubscriptionResponse cancelLocked(UUID userId, CancelMembershipRequest request) {
    requireUser(userId);
    rejectPendingPaymentCancellation(userId);

    var activeSubscription =
        subscriptionFetchService.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    if (activeSubscription.isEmpty()) {
      return responseForPreviouslyCancelledMembership(userId);
    }

    return cancelActiveMembership(activeSubscription.get(), request);
  }

  private MembershipSubscriptionResponse cancelActiveMembership(
      MembershipSubscriptionEntity subscription, CancelMembershipRequest request) {
    var plan = requirePlan(subscription);
    Instant cancelledAt = Instant.now();
    RefundQuote refund = calculateRefund(subscription, plan.getDurationDays(), cancelledAt);

    subscription.cancel(cancelledAt);
    subscription.recordUpdate(request.requestedBy());
    createRefundIfNeeded(subscription, refund, request);
    return userMembershipQueryManager.toResponse(subscription);
  }

  private void rejectPendingPaymentCancellation(UUID userId) {
    if (subscriptionFetchService
        .findByUserIdAndStatus(userId, SubscriptionStatus.PENDING_PAYMENT)
        .isPresent()) {
      throw new ConflictException(
          "A pending membership payment must be completed or retried before cancellation");
    }
  }

  private MembershipSubscriptionResponse responseForPreviouslyCancelledMembership(UUID userId) {
    MembershipSubscriptionEntity latest =
        subscriptionFetchService
            .findLatestByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Active membership", userId));
    if (latest.getStatus() == SubscriptionStatus.CANCELLED) {
      return userMembershipQueryManager.toResponse(latest);
    }
    throw new ResourceNotFoundException("Active membership", userId);
  }

  private com.firstclub.membership.data.entity.MembershipPlanEntity requirePlan(
      MembershipSubscriptionEntity subscription) {
    return planFetchService.getRequired(subscription.getMembershipPlanId());
  }

  private RefundQuote calculateRefund(
      MembershipSubscriptionEntity subscription, int durationDays, Instant referenceTime) {
    if (subscription.getOrigin() == SubscriptionOrigin.CRITERIA_GRANTED) {
      return new RefundQuote(0, durationDays, BigDecimal.ZERO);
    }
    return refundCalculator.calculate(
        subscription.getMembershipValueAmount(),
        durationDays,
        subscription.getActivationTimestamp(),
        referenceTime);
  }

  private void createRefundIfNeeded(
      MembershipSubscriptionEntity subscription, RefundQuote quote, CancelMembershipRequest request) {
    if (quote.amount().compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    if (refundFetchService.findBySubscriptionId(subscription.getId()).isPresent()) {
      return;
    }

    RefundEntity refund = createRefund(subscription, quote, request);
    createRefundAllocations(subscription, quote, refund, request.requestedBy());
  }

  private RefundEntity createRefund(
      MembershipSubscriptionEntity subscription, RefundQuote quote, CancelMembershipRequest request) {
    RefundEntity refund =
        new RefundEntity(
            subscription.getId(),
            quote.amount(),
            subscription.getCurrencyCode(),
            cancellationReason(request),
            RefundStatus.PENDING);
    refund.recordCreation(request.requestedBy());
    return refundWriteService.save(refund);
  }

  private void createRefundAllocations(
      MembershipSubscriptionEntity subscription, RefundQuote quote, RefundEntity refund, String actor) {
    for (var allocation :
        transactionIdentificationService.largestFirst(subscription.getId(), quote.amount())) {
      RefundAllocationEntity refundAllocation =
          new RefundAllocationEntity(
              refund.getId(),
              allocation.sourcePaymentId(),
              allocation.amount(),
              subscription.getCurrencyCode(),
              RefundStatus.PENDING);
      refundAllocation.recordCreation(actor);
      refundAllocationWriteService.save(refundAllocation);
    }
  }

  private String cancellationReason(CancelMembershipRequest request) {
    if (request == null || request.reason() == null || request.reason().isBlank()) {
      return "USER_CANCELLED";
    }
    return request.reason().trim();
  }

  private MembershipSubscriptionEntity requireActive(UUID userId) {
    return subscriptionFetchService
        .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Active membership", userId));
  }

  private void requireUser(UUID userId) {
    if (!userServiceInterface.userExists(userId))
      throw new ResourceNotFoundException("User", userId);
  }
}
