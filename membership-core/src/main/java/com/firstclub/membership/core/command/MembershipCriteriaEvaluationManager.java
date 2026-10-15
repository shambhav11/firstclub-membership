package com.firstclub.membership.core.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.OrderServiceInterface;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.criteria.EvaluationWindowTimestampHelper;
import com.firstclub.membership.core.query.UserMembershipQueryManager;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.MembershipTierCriteriaFetchService;
import com.firstclub.membership.core.query.MembershipTierFetchService;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.models.CriteriaEvaluationOutcome;
import com.firstclub.membership.models.CriteriaOperator;
import com.firstclub.membership.models.CriteriaParameter;
import com.firstclub.membership.models.EvaluationWindow;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.SubscriptionOrigin;
import com.firstclub.membership.models.SubscriptionStatus;
import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.response.user.CriteriaEvaluationResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the highest-priority matching tier criterion to a user without creating a payment. */
@Service
public class MembershipCriteriaEvaluationManager {
  private final UserServiceInterface userServiceInterface;
  private final OrderServiceInterface orderServiceInterface;
  private final EvaluationWindowTimestampHelper evaluationWindowTimestampHelper;
  private final UserCheckoutTransactionExecutor transactionExecutor;
  private final MembershipTierCriteriaFetchService criteriaFetchService;
  private final MembershipTierFetchService tierFetchService;
  private final MembershipPlanFetchService planFetchService;
  private final MembershipSubscriptionFetchService subscriptionFetchService;
  private final MembershipSubscriptionWriteService subscriptionWriteService;
  private final UserMembershipQueryManager userMembershipQueryManager;

  public MembershipCriteriaEvaluationManager(
      UserServiceInterface userServiceInterface,
      OrderServiceInterface orderServiceInterface,
      EvaluationWindowTimestampHelper evaluationWindowTimestampHelper,
      UserCheckoutTransactionExecutor transactionExecutor,
      MembershipTierCriteriaFetchService criteriaFetchService,
      MembershipTierFetchService tierFetchService,
      MembershipPlanFetchService planFetchService,
      MembershipSubscriptionFetchService subscriptionFetchService,
      MembershipSubscriptionWriteService subscriptionWriteService,
      UserMembershipQueryManager userMembershipQueryManager) {
    this.userServiceInterface = userServiceInterface;
    this.orderServiceInterface = orderServiceInterface;
    this.evaluationWindowTimestampHelper = evaluationWindowTimestampHelper;
    this.transactionExecutor = transactionExecutor;
    this.criteriaFetchService = criteriaFetchService;
    this.tierFetchService = tierFetchService;
    this.planFetchService = planFetchService;
    this.subscriptionFetchService = subscriptionFetchService;
    this.subscriptionWriteService = subscriptionWriteService;
    this.userMembershipQueryManager = userMembershipQueryManager;
  }

  public CriteriaEvaluationResponse evaluate(UUID userId, String actor) {
    return transactionExecutor.execute(
        userId,
        () -> {
          if (!userServiceInterface.userExists(userId)) {
            throw new ResourceNotFoundException("User", userId);
          }
          return evaluateLocked(userId, actor);
        });
  }

  private CriteriaEvaluationResponse evaluateLocked(UUID userId, String actor) {
    rejectPendingPaymentEvaluation(userId);
    Match match = findFirstMatch(userId);
    if (match == null)
      return new CriteriaEvaluationResponse(
          CriteriaEvaluationOutcome.NO_MATCH, null, null, null, null);

    MembershipSubscriptionEntity current = findActiveSubscription(userId);
    if (current != null && current.getOrigin() == SubscriptionOrigin.PURCHASED) {
      return response(
          CriteriaEvaluationOutcome.PAID_MEMBERSHIP_ACTIVE,
          match,
          userMembershipQueryManager.toResponse(current));
    }
    if (current != null && current.getMembershipPlanId().equals(match.plan().getId())) {
      return response(
          CriteriaEvaluationOutcome.ALREADY_GRANTED,
          match,
          userMembershipQueryManager.toResponse(current));
    }
    return grantCriteriaMembership(userId, actor, match, current);
  }

  private void rejectPendingPaymentEvaluation(UUID userId) {
    if (subscriptionFetchService
        .findByUserIdAndStatus(userId, SubscriptionStatus.PENDING_PAYMENT)
        .isPresent()) {
      throw new ConflictException(
          "A pending membership payment must be completed or retried before criteria evaluation");
    }
  }

  private Match findFirstMatch(UUID userId) {
    List<MembershipTierCriteriaEntity> criteria =
        criteriaFetchService.findAllOrderedByPriority();
    Map<UUID, MembershipTierEntity> tiersById = findTiersByCriteria(criteria);
    Map<UUID, MembershipPlanEntity> plansById = findPlansByCriteria(criteria);
    CriterionFacts facts = new CriterionFacts(userId);
    return criteria.stream()
        .map(candidate -> resolveMatch(candidate, tiersById, plansById, facts))
        .flatMap(java.util.Optional::stream)
        .findFirst()
        .orElse(null);
  }

  private Map<UUID, MembershipTierEntity> findTiersByCriteria(
      List<MembershipTierCriteriaEntity> criteria) {
    return tierFetchService
        .findAllByIds(
            criteria.stream()
                .map(MembershipTierCriteriaEntity::getMembershipTierId)
                .collect(java.util.stream.Collectors.toSet()))
        .stream()
        .collect(java.util.stream.Collectors.toMap(MembershipTierEntity::getId, tier -> tier));
  }

  private Map<UUID, MembershipPlanEntity> findPlansByCriteria(
      List<MembershipTierCriteriaEntity> criteria) {
    return planFetchService
        .findAllByIds(
            criteria.stream()
                .map(MembershipTierCriteriaEntity::getGrantedPlanId)
                .collect(java.util.stream.Collectors.toSet()))
        .stream()
        .collect(java.util.stream.Collectors.toMap(MembershipPlanEntity::getId, plan -> plan));
  }

  private MembershipSubscriptionEntity findActiveSubscription(UUID userId) {
    return subscriptionFetchService
        .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
        .orElse(null);
  }

  private CriteriaEvaluationResponse grantCriteriaMembership(
      UUID userId, String actor, Match match, MembershipSubscriptionEntity current) {
    Instant now = Instant.now();
    cancelExistingCriteriaMembership(current, actor, now);
    MembershipSubscriptionEntity granted =
        MembershipSubscriptionEntity.criteriaGranted(
            userId,
            match.plan().getId(),
            match.plan().getCurrencyCode(),
            now,
            now.plus(Duration.ofDays(match.plan().getDurationDays())));
    granted.recordCreation(actor);
    subscriptionWriteService.save(granted);
    subscriptionWriteService.flush();
    return response(
        CriteriaEvaluationOutcome.GRANTED, match, userMembershipQueryManager.toResponse(granted));
  }

  private void cancelExistingCriteriaMembership(
      MembershipSubscriptionEntity current, String actor, Instant now) {
    if (current == null) {
      return;
    }
    current.cancel(now);
    current.recordUpdate(actor);
    subscriptionWriteService.flush();
  }

  private java.util.Optional<Match> resolveMatch(
      MembershipTierCriteriaEntity candidate,
      Map<UUID, MembershipTierEntity> tiersById,
      Map<UUID, MembershipPlanEntity> plansById,
      CriterionFacts facts) {
    MembershipTierEntity tier = tiersById.get(candidate.getMembershipTierId());
    MembershipPlanEntity plan = plansById.get(candidate.getGrantedPlanId());
    if (tier == null
        || plan == null
        || !plan.getMembershipTierId().equals(tier.getId())
        || tier.getStatus() != TierStatus.ACTIVE
        || tier.isEnrollmentClosed()
        || plan.getStatus() != PlanStatus.ACTIVE
        || plan.isEnrollmentClosed()) return java.util.Optional.empty();
    return matches(candidate.getExpression(), candidate, facts)
        ? java.util.Optional.of(new Match(tier, plan))
        : java.util.Optional.empty();
  }

  private boolean matches(
      JsonNode node, MembershipTierCriteriaEntity criterion, CriterionFacts facts) {
    JsonNode children = node.path("children");
    CriteriaOperator operator = enumValue(CriteriaOperator.class, node, "operator");
    if (children.isArray() && !children.isEmpty()) {
      if (operator == CriteriaOperator.AND) {
        for (JsonNode child : children) if (!matches(child, criterion, facts)) return false;
        return true;
      }
      if (operator == CriteriaOperator.OR) {
        for (JsonNode child : children) if (matches(child, criterion, facts)) return true;
        return false;
      }
      throw new ConflictException("Stored criteria group has an invalid operator");
    }
    CriteriaParameter parameter = enumValue(CriteriaParameter.class, node, "parameter");
    JsonNode threshold = node.get("threshold");
    if (threshold == null)
      throw new ConflictException("Stored criteria condition has no threshold");
    return switch (parameter) {
      case ORDER_COUNT ->
          compare(
              BigDecimal.valueOf(facts.orderCount(criterion.getEvaluationWindow())),
              threshold,
              operator);
      case ORDER_VALUE ->
          compare(facts.orderValue(criterion.getEvaluationWindow()), threshold, operator);
      case COHORT -> cohortMatches(facts.cohorts(), threshold, operator);
    };
  }

  private boolean compare(BigDecimal actual, JsonNode threshold, CriteriaOperator operator) {
    if (!threshold.isNumber())
      throw new ConflictException("Stored order criterion has a non-numeric threshold");
    int comparison = actual.compareTo(threshold.decimalValue());
    return operator == CriteriaOperator.GREATER_THAN
        ? comparison > 0
        : operator == CriteriaOperator.LESS_THAN && comparison < 0;
  }

  private boolean cohortMatches(
      Set<String> cohorts, JsonNode threshold, CriteriaOperator operator) {
    if (!threshold.isArray())
      throw new ConflictException("Stored cohort criterion has an invalid threshold");
    Set<String> expected = new java.util.HashSet<>();
    threshold.forEach(value -> expected.add(value.asText()));
    return operator == CriteriaOperator.IN_ANY
        ? cohorts.stream().anyMatch(expected::contains)
        : operator == CriteriaOperator.IN_ALL && cohorts.containsAll(expected);
  }

  private <T extends Enum<T>> T enumValue(Class<T> type, JsonNode node, String field) {
    try {
      return Enum.valueOf(type, node.path(field).asText());
    } catch (IllegalArgumentException exception) {
      throw new ConflictException("Stored criteria has an invalid " + field);
    }
  }

  private CriteriaEvaluationResponse response(
      CriteriaEvaluationOutcome outcome,
      Match match,
      com.firstclub.membership.models.response.user.MembershipSubscriptionResponse membership) {
    return new CriteriaEvaluationResponse(
        outcome,
        match.tier().getId(),
        match.tier().getDisplayName(),
        match.plan().getId(),
        membership);
  }

  private record Match(MembershipTierEntity tier, MembershipPlanEntity plan) {}

  private final class CriterionFacts {
    private final UUID userId;
    private final Map<EvaluationWindow, Long> orderCounts = new java.util.HashMap<>();
    private final Map<EvaluationWindow, BigDecimal> orderValues = new java.util.HashMap<>();
    private Set<String> cohorts;

    private CriterionFacts(UUID userId) {
      this.userId = userId;
    }

    private long orderCount(EvaluationWindow evaluationWindow) {
      return orderCounts.computeIfAbsent(
          evaluationWindow,
          window -> {
            var range = evaluationWindowTimestampHelper.forWindow(window);
            return (long)
                orderServiceInterface.getOrderCountForUserBetweenTimestamps(
                    userId, range.fromInclusive(), range.toExclusive());
          });
    }

    private BigDecimal orderValue(EvaluationWindow evaluationWindow) {
      return orderValues.computeIfAbsent(
          evaluationWindow,
          window -> {
            var range = evaluationWindowTimestampHelper.forWindow(window);
            return orderServiceInterface.getOrderValueForUserBetweenTimestamps(
                userId, range.fromInclusive(), range.toExclusive());
          });
    }

    private Set<String> cohorts() {
      if (cohorts == null) cohorts = userServiceInterface.getUserCohorts(userId);
      return cohorts;
    }
  }
}
