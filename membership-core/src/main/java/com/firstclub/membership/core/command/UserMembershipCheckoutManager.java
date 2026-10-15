package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.MembershipTierFetchService;
import com.firstclub.membership.core.query.PaymentFetchService;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.data.mappers.UserMembershipResponseMapper;
import com.firstclub.membership.models.PaymentStatus;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.SubscriptionStatus;
import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.request.user.SubscribeMembershipRequest;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserMembershipCheckoutManager {
  private static final Duration PAYMENT_EXPIRY = Duration.ofMinutes(15);
  private final UserCheckoutTransactionExecutor transactionExecutor;
  private final MembershipPlanFetchService planFetchService;
  private final MembershipTierFetchService tierFetchService;
  private final MembershipSubscriptionFetchService subscriptionFetchService;
  private final PaymentFetchService paymentFetchService;
  private final MembershipSubscriptionWriteService subscriptionWriteService;
  private final PaymentWriteService paymentWriteService;
  private final UserServiceInterface userServiceInterface;
  private final UserMembershipResponseMapper responseMapper;
  private final MembershipMaintenanceManager membershipMaintenanceManager;

  public UserMembershipCheckoutManager(
      UserCheckoutTransactionExecutor transactionExecutor,
      MembershipPlanFetchService planFetchService,
      MembershipTierFetchService tierFetchService,
      MembershipSubscriptionFetchService subscriptionFetchService,
      PaymentFetchService paymentFetchService,
      MembershipSubscriptionWriteService subscriptionWriteService,
      PaymentWriteService paymentWriteService,
      UserServiceInterface userServiceInterface,
      UserMembershipResponseMapper responseMapper,
      MembershipMaintenanceManager membershipMaintenanceManager) {
    this.transactionExecutor = transactionExecutor;
    this.planFetchService = planFetchService;
    this.tierFetchService = tierFetchService;
    this.subscriptionFetchService = subscriptionFetchService;
    this.paymentFetchService = paymentFetchService;
    this.subscriptionWriteService = subscriptionWriteService;
    this.paymentWriteService = paymentWriteService;
    this.userServiceInterface = userServiceInterface;
    this.responseMapper = responseMapper;
    this.membershipMaintenanceManager = membershipMaintenanceManager;
  }

  public MembershipSubscriptionResponse subscribe(UUID userId, SubscribeMembershipRequest request) {
    return transactionExecutor.execute(
        userId,
        () -> {
          requireUser(userId);
          return createPendingSubscription(userId, request);
        });
  }

  public MembershipSubscriptionResponse completePayment(
      UUID paymentId, String bankReferenceId, String actor) {
    return transactionExecutor.execute(
        paymentUserId(paymentId), () -> completePaymentInTransaction(paymentId, bankReferenceId, actor));
  }

  private MembershipSubscriptionResponse completePaymentInTransaction(
      UUID paymentId, String bankReferenceId, String actor) {
    PaymentEntity payment = requirePaymentForUpdate(paymentId);
    if (payment.getStatus() == PaymentStatus.SUCCESS) {
      return responseForPayment(payment);
    }
    requireCompletablePayment(payment);
    MembershipSubscriptionEntity subscription = requireSubscription(payment.getSubscriptionId());
    MembershipPlanEntity plan = requirePlan(subscription.getMembershipPlanId());
    if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
      completeRenewalPayment(subscription, payment, bankReferenceId, actor);
      return responseForPayment(payment);
    }
    completeInitialPayment(subscription, plan, payment, bankReferenceId, actor);
    return responseForPayment(payment);
  }

  private void requireCompletablePayment(PaymentEntity payment) {
    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new ConflictException("Payment is no longer pending");
    }
    if (payment.getPaymentExpiresAt() != null && !payment.getPaymentExpiresAt().isAfter(Instant.now())) {
      throw new ConflictException("Payment has expired; create a new payment attempt");
    }
  }

  private void completeRenewalPayment(
      MembershipSubscriptionEntity subscription,
      PaymentEntity payment,
      String bankReferenceId,
      String actor) {
    membershipMaintenanceManager.completeAutoRenewalPayment(subscription, payment, bankReferenceId);
    subscription.recordUpdate(actor);
    payment.recordUpdate(actor);
  }

  private void completeInitialPayment(
      MembershipSubscriptionEntity subscription,
      MembershipPlanEntity plan,
      PaymentEntity payment,
      String bankReferenceId,
      String actor) {
    if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
      throw new ConflictException("Payment does not belong to a pending or renewable membership");
    }
    Instant activation = Instant.now();
    cancelCurrentActiveSubscription(subscription, activation, actor);
    subscription.activate(
        activation, activation.plus(Duration.ofDays(plan.getDurationDays())), payment.getAmount());
    payment.complete(bankReferenceId);
    subscription.recordUpdate(actor);
    payment.recordUpdate(actor);
  }

  private void cancelCurrentActiveSubscription(
      MembershipSubscriptionEntity subscription, Instant activation, String actor) {
    subscriptionFetchService
        .findByUserIdAndStatus(subscription.getUserId(), SubscriptionStatus.ACTIVE)
        .ifPresent(activeSubscription -> cancelForReplacement(activeSubscription, activation, actor));
  }

  private void cancelForReplacement(
      MembershipSubscriptionEntity activeSubscription, Instant activation, String actor) {
    activeSubscription.cancel(activation);
    activeSubscription.recordUpdate(actor);
    // PostgreSQL checks the partial active-subscription index per statement.
    subscriptionWriteService.flush();
  }

  public MembershipSubscriptionResponse failPayment(UUID paymentId, String actor) {
    return transactionExecutor.execute(
        paymentUserId(paymentId), () -> failPaymentInTransaction(paymentId, actor));
  }

  private MembershipSubscriptionResponse failPaymentInTransaction(UUID paymentId, String actor) {
    PaymentEntity payment = requirePaymentForUpdate(paymentId);
    if (payment.getStatus() == PaymentStatus.FAILED) {
      return responseForPayment(payment);
    }
    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new ConflictException("Payment is no longer pending");
    }
    payment.fail();
    payment.recordUpdate(actor);
    return responseForPayment(payment);
  }

  public MembershipSubscriptionResponse retryPayment(UUID userId, UUID subscriptionId, String actor) {
    return transactionExecutor.execute(
        userId, () -> retryPaymentInTransaction(userId, subscriptionId, actor));
  }

  private MembershipSubscriptionResponse retryPaymentInTransaction(
      UUID userId, UUID subscriptionId, String actor) {
    requireUser(userId);
    MembershipSubscriptionEntity subscription = requirePendingSubscription(userId, subscriptionId);
    expirePreviousPayment(subscriptionId, actor);
    PaymentEntity payment = createRetryPayment(subscription, actor);
    return responseForPayment(payment);
  }

  private MembershipSubscriptionEntity requirePendingSubscription(UUID userId, UUID subscriptionId) {
    MembershipSubscriptionEntity subscription =
        subscriptionFetchService
            .findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership subscription", subscriptionId));
    if (!subscription.getUserId().equals(userId)) {
      throw new ResourceNotFoundException("Membership subscription", subscriptionId);
    }
    if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
      throw new ConflictException("Only a pending membership can be retried");
    }
    return subscription;
  }

  private void expirePreviousPayment(UUID subscriptionId, String actor) {
    PaymentEntity payment =
        paymentFetchService
            .findLatestBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new ConflictException("No payment exists for this membership"));
    if (payment.getStatus() == PaymentStatus.SUCCESS) {
      throw new ConflictException("Membership payment has already succeeded");
    }
    if (payment.getStatus() == PaymentStatus.PENDING) {
      expirePendingPayment(payment, actor);
    }
  }

  private void expirePendingPayment(PaymentEntity payment, String actor) {
    if (payment.getPaymentExpiresAt() == null || payment.getPaymentExpiresAt().isAfter(Instant.now())) {
      throw new ConflictException("A membership payment is already pending for this user");
    }
    payment.expire();
    payment.recordUpdate(actor);
  }

  private PaymentEntity createRetryPayment(MembershipSubscriptionEntity subscription, String actor) {
    PaymentEntity payment =
        new PaymentEntity(
            subscription.getId(),
            UUID.randomUUID().toString(),
            subscription.getMembershipValueAmount().subtract(subscription.getCreditAppliedAmount()),
            subscription.getCurrencyCode(),
            Instant.now().plus(PAYMENT_EXPIRY));
    payment.recordCreation(actor);
    return paymentWriteService.save(payment);
  }

  private PaymentEntity requirePaymentForUpdate(UUID paymentId) {
    return paymentFetchService
        .findByIdForUpdate(paymentId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
  }

  private MembershipSubscriptionResponse createPendingSubscription(
      UUID userId, SubscribeMembershipRequest request) {
    rejectExistingMembership(userId);
    MembershipPlanEntity plan = requirePlan(request.membershipPlanId());
    MembershipTierEntity tier = requireAvailableTier(plan);
    MembershipSubscriptionEntity subscription = createPendingSubscription(userId, plan, request);
    PaymentEntity payment = createInitialPayment(subscription, plan, request.requestedBy());
    subscriptionWriteService.flush();
    return toResponse(subscription, plan, tier, payment);
  }

  private void rejectExistingMembership(UUID userId) {
    if (subscriptionFetchService.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).isPresent()) {
      throw new ConflictException("User already has an active membership");
    }
    if (subscriptionFetchService
        .findByUserIdAndStatus(userId, SubscriptionStatus.PENDING_PAYMENT)
        .isPresent()) {
      throw new ConflictException("A membership payment is already pending for this user");
    }
  }

  private MembershipTierEntity requireAvailableTier(MembershipPlanEntity plan) {
    MembershipTierEntity tier = tierFetchService.getRequired(plan.getMembershipTierId());
    if (tier.getStatus() != TierStatus.ACTIVE
        || tier.isEnrollmentClosed()
        || plan.getStatus() != PlanStatus.ACTIVE
        || plan.isEnrollmentClosed()) {
      throw new ConflictException("The selected membership plan is not available for enrollment");
    }
    return tier;
  }

  private MembershipSubscriptionEntity createPendingSubscription(
      UUID userId, MembershipPlanEntity plan, SubscribeMembershipRequest request) {
    MembershipSubscriptionEntity subscription =
        new MembershipSubscriptionEntity(
            userId,
            plan.getId(),
            plan.getAmount(),
            java.math.BigDecimal.ZERO,
            plan.getCurrencyCode(),
            request.autoRenew());
    subscription.recordCreation(request.requestedBy());
    return subscriptionWriteService.save(subscription);
  }

  private PaymentEntity createInitialPayment(
      MembershipSubscriptionEntity subscription, MembershipPlanEntity plan, String actor) {
    PaymentEntity payment =
        new PaymentEntity(
            subscription.getId(),
            UUID.randomUUID().toString(),
            plan.getAmount(),
            plan.getCurrencyCode(),
            Instant.now().plus(PAYMENT_EXPIRY));
    payment.recordCreation(actor);
    return paymentWriteService.save(payment);
  }

  private MembershipSubscriptionResponse toResponse(
      MembershipSubscriptionEntity subscription,
      MembershipPlanEntity plan,
      MembershipTierEntity tier,
      PaymentEntity payment) {
    return responseMapper.toSubscription(subscription, plan, tier, payment, null);
  }

  private MembershipSubscriptionResponse responseForPayment(PaymentEntity payment) {
    MembershipSubscriptionEntity subscription = requireSubscription(payment.getSubscriptionId());
    MembershipPlanEntity plan = requirePlan(subscription.getMembershipPlanId());
    MembershipTierEntity tier = tierFetchService.getRequired(plan.getMembershipTierId());
    return toResponse(subscription, plan, tier, payment);
  }

  private MembershipSubscriptionEntity requireSubscription(UUID subscriptionId) {
    return subscriptionFetchService
        .findById(subscriptionId)
        .orElseThrow(() -> new ResourceNotFoundException("Membership subscription", subscriptionId));
  }

  private MembershipPlanEntity requirePlan(UUID planId) {
    return planFetchService.getRequired(planId);
  }

  private UUID paymentUserId(UUID paymentId) {
    PaymentEntity payment =
        paymentFetchService
            .findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    return subscriptionFetchService
        .findById(payment.getSubscriptionId())
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Membership subscription", payment.getSubscriptionId()))
        .getUserId();
  }

  private void requireUser(UUID userId) {
    if (!userServiceInterface.userExists(userId))
      throw new ResourceNotFoundException("User", userId);
  }
}
