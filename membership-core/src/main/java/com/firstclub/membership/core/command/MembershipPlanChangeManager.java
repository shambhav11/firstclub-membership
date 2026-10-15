package com.firstclub.membership.core.command;

import com.firstclub.membership.core.query.UserMembershipQueryManager;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.refund.ProratedRefundCalculator;
import com.firstclub.membership.core.refund.RefundTransactionIdentificationService;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.data.entity.RefundAllocationEntity;
import com.firstclub.membership.data.entity.RefundEntity;
import com.firstclub.membership.models.RefundStatus;
import com.firstclub.membership.models.request.user.ChangeMembershipPlanRequest;
import com.firstclub.membership.models.response.user.MembershipChangeQuoteResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipPlanChangeManager {
  private final MembershipPlanChangeValidator membershipPlanChangeValidator;
  private final ProratedRefundCalculator proratedRefundCalculator;
  private final UserCheckoutTransactionExecutor transactionExecutor;
  private final MembershipSubscriptionWriteService subscriptionWriteService;
  private final PaymentWriteService paymentWriteService;
  private final MembershipPlanFetchService planFetchService;
  private final RefundTransactionIdentificationService refundTransactionIdentificationService;
  private final RefundWriteService refundWriteService;
  private final RefundAllocationWriteService refundAllocationWriteService;
  private final UserMembershipQueryManager userMembershipQueryManager;

  public MembershipPlanChangeManager(
      MembershipPlanChangeValidator membershipPlanChangeValidator,
      ProratedRefundCalculator proratedRefundCalculator,
      UserCheckoutTransactionExecutor transactionExecutor,
      MembershipSubscriptionWriteService subscriptionWriteService,
      PaymentWriteService paymentWriteService,
      MembershipPlanFetchService planFetchService,
      RefundTransactionIdentificationService refundTransactionIdentificationService,
      RefundWriteService refundWriteService,
      RefundAllocationWriteService refundAllocationWriteService,
      UserMembershipQueryManager userMembershipQueryManager) {
    this.membershipPlanChangeValidator = membershipPlanChangeValidator;
    this.proratedRefundCalculator = proratedRefundCalculator;
    this.transactionExecutor = transactionExecutor;
    this.subscriptionWriteService = subscriptionWriteService;
    this.paymentWriteService = paymentWriteService;
    this.planFetchService = planFetchService;
    this.refundTransactionIdentificationService = refundTransactionIdentificationService;
    this.refundWriteService = refundWriteService;
    this.refundAllocationWriteService = refundAllocationWriteService;
    this.userMembershipQueryManager = userMembershipQueryManager;
  }

  @Transactional(readOnly = true)
  public MembershipChangeQuoteResponse quote(UUID userId, ChangeMembershipPlanRequest request) {
    return quoteFor(membershipPlanChangeValidator.validate(userId, request));
  }

  public MembershipSubscriptionResponse change(UUID userId, ChangeMembershipPlanRequest request) {
    return transactionExecutor.execute(userId, () -> changeLocked(userId, request));
  }

  private MembershipSubscriptionResponse changeLocked(
      UUID userId, ChangeMembershipPlanRequest request) {
    var change = membershipPlanChangeValidator.validate(userId, request);
    var quote = quoteFor(change);
    MembershipSubscriptionEntity next = createReplacement(userId, request, change, quote);
    var sourceAllocations = allocateSources(change, quote);
    createMembershipCredits(next, change, sourceAllocations, request.requestedBy());
    createRefundIfNeeded(next, change, quote, sourceAllocations, request.requestedBy());
    settleOrCollectPayment(next, change, quote, request.requestedBy());
    return userMembershipQueryManager.toResponse(next);
  }

  private MembershipSubscriptionEntity createReplacement(
      UUID userId,
      ChangeMembershipPlanRequest request,
      MembershipPlanChangeValidator.ValidatedPlanChange change,
      MembershipChangeQuoteResponse quote) {
    MembershipSubscriptionEntity next =
        new MembershipSubscriptionEntity(
            userId,
            change.target().getId(),
            change.target().getAmount(),
            quote.creditAppliedAmount(),
            change.target().getCurrencyCode(),
            request.autoRenew());
    next.recordCreation(request.requestedBy());
    return subscriptionWriteService.save(next);
  }

  private com.firstclub.membership.core.refund.RefundTransactionIdentificationService.PlanChangePaymentSourceAllocations
      allocateSources(
          MembershipPlanChangeValidator.ValidatedPlanChange change,
          MembershipChangeQuoteResponse quote) {
    return refundTransactionIdentificationService.allocatePlanChange(
        change.current().getId(), quote.creditAppliedAmount(), quote.refundAmount());
  }

  private void createMembershipCredits(
      MembershipSubscriptionEntity next,
      MembershipPlanChangeValidator.ValidatedPlanChange change,
      com.firstclub.membership.core.refund.RefundTransactionIdentificationService.PlanChangePaymentSourceAllocations allocations,
      String actor) {
    for (var allocation : allocations.creditAllocations()) {
      PaymentEntity payment =
          PaymentEntity.membershipCredit(
              next.getId(),
              allocation.amount(),
              next.getCurrencyCode(),
              change.current().getId(),
              allocation.sourcePaymentId());
      payment.recordCreation(actor);
      paymentWriteService.save(payment);
    }
  }

  private void createRefundIfNeeded(
      MembershipSubscriptionEntity next,
      MembershipPlanChangeValidator.ValidatedPlanChange change,
      MembershipChangeQuoteResponse quote,
      com.firstclub.membership.core.refund.RefundTransactionIdentificationService.PlanChangePaymentSourceAllocations allocations,
      String actor) {
    if (quote.refundAmount().signum() <= 0) {
      return;
    }
    RefundEntity refund = createRefund(next, change, quote, actor);
    createRefundAllocations(next, refund, allocations, actor);
  }

  private RefundEntity createRefund(
      MembershipSubscriptionEntity next,
      MembershipPlanChangeValidator.ValidatedPlanChange change,
      MembershipChangeQuoteResponse quote,
      String actor) {
    RefundEntity refund =
        new RefundEntity(
            change.current().getId(),
            quote.refundAmount(),
            next.getCurrencyCode(),
            "PLAN_CHANGE_SURPLUS",
            RefundStatus.PENDING);
    refund.recordCreation(actor);
    return refundWriteService.save(refund);
  }

  private void createRefundAllocations(
      MembershipSubscriptionEntity next,
      RefundEntity refund,
      com.firstclub.membership.core.refund.RefundTransactionIdentificationService.PlanChangePaymentSourceAllocations allocations,
      String actor) {
    for (var allocation : allocations.refundAllocations()) {
      RefundAllocationEntity refundAllocation =
          new RefundAllocationEntity(
              refund.getId(),
              allocation.sourcePaymentId(),
              allocation.amount(),
              next.getCurrencyCode(),
              RefundStatus.PENDING);
      refundAllocation.recordCreation(actor);
      refundAllocationWriteService.save(refundAllocation);
    }
  }

  private void settleOrCollectPayment(
      MembershipSubscriptionEntity next,
      MembershipPlanChangeValidator.ValidatedPlanChange change,
      MembershipChangeQuoteResponse quote,
      String actor) {
    if (quote.amountDue().signum() > 0) {
      createDirectPayment(next, quote.amountDue(), actor);
      return;
    }
    activateZeroDueReplacement(next, change, actor);
  }

  private void createDirectPayment(MembershipSubscriptionEntity next, BigDecimal amount, String actor) {
    PaymentEntity payment =
        new PaymentEntity(
            next.getId(), UUID.randomUUID().toString(), amount, next.getCurrencyCode(), Instant.now().plusSeconds(900));
    payment.recordCreation(actor);
    paymentWriteService.save(payment);
  }

  private void activateZeroDueReplacement(
      MembershipSubscriptionEntity next,
      MembershipPlanChangeValidator.ValidatedPlanChange change,
      String actor) {
    Instant activation = Instant.now();
    change.current().cancel(activation);
    change.current().recordUpdate(actor);
    subscriptionWriteService.flush();
    next.activate(
        activation,
        activation.plusSeconds(change.target().getDurationDays() * 86400L),
        BigDecimal.ZERO);
    next.recordUpdate(actor);
  }

  private MembershipChangeQuoteResponse quoteFor(
      MembershipPlanChangeValidator.ValidatedPlanChange change) {
    MembershipSubscriptionEntity current = change.current();
    MembershipPlanEntity target = change.target();
    var refund =
        proratedRefundCalculator.calculate(
            current.getMembershipValueAmount(),
            planDuration(current),
            current.getActivationTimestamp(),
            Instant.now());
    BigDecimal credit = refund.amount().min(target.getAmount());
    BigDecimal due = target.getAmount().subtract(credit);
    BigDecimal surplus = refund.amount().subtract(credit);
    return new MembershipChangeQuoteResponse(
        current.getId(),
        current.getMembershipPlanId(),
        target.getId(),
        planDuration(current),
        refund.usedDays(),
        refund.remainingDays(),
        refund.amount(),
        target.getAmount(),
        credit,
        due,
        surplus,
        target.getCurrencyCode());
  }

  private int planDuration(MembershipSubscriptionEntity current) {
    return planFetchService.getRequired(current.getMembershipPlanId()).getDurationDays();
  }
}
