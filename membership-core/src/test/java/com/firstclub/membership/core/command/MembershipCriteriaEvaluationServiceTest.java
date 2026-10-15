package com.firstclub.membership.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.commons.OrderServiceInterface;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.query.UserMembershipQueryManager;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.MembershipTierCriteriaFetchService;
import com.firstclub.membership.core.query.MembershipTierFetchService;
import com.firstclub.membership.core.criteria.EvaluationWindowTimestampHelper;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.models.CriteriaEvaluationOutcome;
import com.firstclub.membership.models.EvaluationWindow;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.SubscriptionOrigin;
import com.firstclub.membership.models.SubscriptionStatus;
import com.firstclub.membership.models.TierStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MembershipCriteriaEvaluationManagerTest {
  private final UserServiceInterface users = mock(UserServiceInterface.class);
  private final OrderServiceInterface orders = mock(OrderServiceInterface.class);
  private final MembershipTierCriteriaFetchService criteria =
      mock(MembershipTierCriteriaFetchService.class);
  private final MembershipTierFetchService tiers = mock(MembershipTierFetchService.class);
  private final MembershipPlanFetchService plans = mock(MembershipPlanFetchService.class);
  private final MembershipSubscriptionFetchService subscriptions =
      mock(MembershipSubscriptionFetchService.class);
  private final MembershipSubscriptionWriteService subscriptionWrites =
      mock(MembershipSubscriptionWriteService.class);
  private final UserMembershipQueryManager reads = mock(UserMembershipQueryManager.class);
  private final UserCheckoutTransactionExecutor transactionExecutor = mock(UserCheckoutTransactionExecutor.class);
  private final EvaluationWindowTimestampHelper evaluationWindowTimestampHelper =
      new EvaluationWindowTimestampHelper(
          Clock.fixed(Instant.parse("2026-10-15T06:30:00Z"), ZoneOffset.UTC));
  private final MembershipCriteriaEvaluationManager service =
      new MembershipCriteriaEvaluationManager(
          users,
          orders,
          evaluationWindowTimestampHelper,
          transactionExecutor,
          criteria,
          tiers,
          plans,
          subscriptions,
          subscriptionWrites,
          reads);

  @BeforeEach
  void setUp() {
    executeTransactionsImmediately();
  }

  @Test
  void grantsTheHighestPriorityMatchingTierWithoutAPayment() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID tierId = UUID.randomUUID();
    MembershipTierCriteriaEntity highPriority = criterion(tierId, 1, 5);
    MembershipTierCriteriaEntity lowerPriority = criterion(UUID.randomUUID(), 2, 1);
    MembershipTierEntity tier = new MembershipTierEntity("Platinum", TierStatus.ACTIVE, false);
    ReflectionTestUtils.setField(tier, "id", tierId);
    MembershipPlanEntity plan =
        new MembershipPlanEntity(
            tierId, 90, "Quarterly", new BigDecimal("3000"), "INR", PlanStatus.ACTIVE, false);
    UUID planId = UUID.randomUUID();
    ReflectionTestUtils.setField(plan, "id", planId);
    ReflectionTestUtils.setField(highPriority, "grantedPlanId", planId);

    when(users.userExists(userId)).thenReturn(true);
    when(
            orders.getOrderCountForUserBetweenTimestamps(
                eq(userId), any(Instant.class), any(Instant.class)))
        .thenReturn(10);
    when(criteria.findAllOrderedByPriority()).thenReturn(List.of(highPriority, lowerPriority));
    when(tiers.findAllByIds(any())).thenReturn(List.of(tier));
    when(plans.findAllByIds(any())).thenReturn(List.of(plan));
    when(subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.PENDING_PAYMENT))
        .thenReturn(Optional.empty());
    when(subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.empty());
    when(subscriptionWrites.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.evaluate(userId, "test-actor");

    assertEquals(CriteriaEvaluationOutcome.GRANTED, result.outcome());
    assertEquals(tierId, result.matchedTierId());
    ArgumentCaptor<MembershipSubscriptionEntity> saved =
        ArgumentCaptor.forClass(MembershipSubscriptionEntity.class);
    verify(subscriptionWrites).save(saved.capture());
    assertEquals(SubscriptionOrigin.CRITERIA_GRANTED, saved.getValue().getOrigin());
    assertEquals(BigDecimal.ZERO, saved.getValue().getMembershipValueAmount());
    assertEquals(BigDecimal.ZERO, saved.getValue().getAmountPaid());
    assertFalse(saved.getValue().isAutoRenew());
  }

  @Test
  void replacesAnExistingFreeMembershipWhenAHigherPriorityCriterionMatches() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID platinumTierId = UUID.randomUUID();
    UUID goldTierId = UUID.randomUUID();
    UUID platinumPlanId = UUID.randomUUID();
    UUID goldPlanId = UUID.randomUUID();
    MembershipTierCriteriaEntity platinumCriterion = criterion(platinumTierId, 1, 5);
    MembershipTierCriteriaEntity goldCriterion = criterion(goldTierId, 2, 1);
    ReflectionTestUtils.setField(platinumCriterion, "grantedPlanId", platinumPlanId);
    ReflectionTestUtils.setField(goldCriterion, "grantedPlanId", goldPlanId);
    MembershipTierEntity platinum = tier(platinumTierId, "Platinum");
    MembershipTierEntity gold = tier(goldTierId, "Gold");
    MembershipPlanEntity platinumPlan = plan(platinumTierId, platinumPlanId, "Platinum Quarterly");
    MembershipPlanEntity goldPlan = plan(goldTierId, goldPlanId, "Gold Quarterly");
    MembershipSubscriptionEntity current =
        MembershipSubscriptionEntity.criteriaGranted(
            userId,
            goldPlanId,
            "INR",
            java.time.Instant.now().minus(java.time.Duration.ofDays(1)),
            java.time.Instant.now().plus(java.time.Duration.ofDays(89)));

    when(users.userExists(userId)).thenReturn(true);
    when(
            orders.getOrderCountForUserBetweenTimestamps(
                eq(userId), any(Instant.class), any(Instant.class)))
        .thenReturn(10);
    when(criteria.findAllOrderedByPriority()).thenReturn(List.of(platinumCriterion, goldCriterion));
    when(tiers.findAllByIds(any())).thenReturn(List.of(platinum, gold));
    when(plans.findAllByIds(any())).thenReturn(List.of(platinumPlan, goldPlan));
    when(subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.PENDING_PAYMENT))
        .thenReturn(Optional.empty());
    when(subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.of(current));
    when(subscriptionWrites.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.evaluate(userId, "test-actor");

    assertEquals(CriteriaEvaluationOutcome.GRANTED, result.outcome());
    assertEquals(platinumTierId, result.matchedTierId());
    assertEquals(SubscriptionStatus.CANCELLED, current.getStatus());
    ArgumentCaptor<MembershipSubscriptionEntity> saved =
        ArgumentCaptor.forClass(MembershipSubscriptionEntity.class);
    verify(subscriptionWrites).save(saved.capture());
    assertEquals(platinumPlanId, saved.getValue().getMembershipPlanId());
    assertEquals(SubscriptionOrigin.CRITERIA_GRANTED, saved.getValue().getOrigin());
  }

  private MembershipTierEntity tier(UUID tierId, String displayName) {
    MembershipTierEntity tier = new MembershipTierEntity(displayName, TierStatus.ACTIVE, false);
    ReflectionTestUtils.setField(tier, "id", tierId);
    return tier;
  }

  private MembershipPlanEntity plan(UUID tierId, UUID planId, String displayText) {
    MembershipPlanEntity plan =
        new MembershipPlanEntity(
            tierId, 90, displayText, new BigDecimal("3000"), "INR", PlanStatus.ACTIVE, false);
    ReflectionTestUtils.setField(plan, "id", planId);
    return plan;
  }

  private MembershipTierCriteriaEntity criterion(UUID tierId, int priority, int minimumOrders)
      throws Exception {
    var expression =
        new ObjectMapper()
            .readTree(
                """
                {"parameter":"ORDER_COUNT","operator":"GREATER_THAN","threshold":%d,"children":[]}
                """
                    .formatted(minimumOrders));
    return new MembershipTierCriteriaEntity(
        tierId, priority, UUID.randomUUID(), EvaluationWindow.MONTHLY, expression);
  }

  @SuppressWarnings("unchecked")
  private void executeTransactionsImmediately() {
    when(transactionExecutor.execute(any(), any()))
        .thenAnswer(invocation -> ((java.util.function.Supplier<Object>) invocation.getArgument(1)).get());
  }
}
