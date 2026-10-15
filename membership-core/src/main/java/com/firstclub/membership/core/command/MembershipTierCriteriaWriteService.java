package com.firstclub.membership.core.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import com.firstclub.membership.data.mappers.AdminMembershipResponseMapper;
import com.firstclub.membership.data.repository.MembershipPlanRepository;
import com.firstclub.membership.data.repository.MembershipTierCriteriaRepository;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.CriteriaOperator;
import com.firstclub.membership.models.request.admin.CriteriaExpressionRequest;
import com.firstclub.membership.models.request.admin.UpsertMembershipTierCriteriaRequest;
import com.firstclub.membership.models.response.admin.MembershipTierCriteriaResponse;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipTierCriteriaWriteService {
  private static final int MAX_EXPRESSION_DEPTH = 10;
  private final MembershipTierRepository membershipTierRepository;
  private final MembershipPlanRepository membershipPlanRepository;
  private final MembershipTierCriteriaRepository membershipTierCriteriaRepository;
  private final ObjectMapper objectMapper;
  private final AdminMembershipResponseMapper adminMembershipResponseMapper;

  public MembershipTierCriteriaWriteService(
      MembershipTierRepository membershipTierRepository,
      MembershipPlanRepository membershipPlanRepository,
      MembershipTierCriteriaRepository membershipTierCriteriaRepository,
      ObjectMapper objectMapper,
      AdminMembershipResponseMapper adminMembershipResponseMapper) {
    this.membershipTierRepository = membershipTierRepository;
    this.membershipPlanRepository = membershipPlanRepository;
    this.membershipTierCriteriaRepository = membershipTierCriteriaRepository;
    this.objectMapper = objectMapper;
    this.adminMembershipResponseMapper = adminMembershipResponseMapper;
  }

  @Transactional
  public MembershipTierCriteriaResponse upsert(
      UUID tierId, UpsertMembershipTierCriteriaRequest request) {
    requirePlanForTier(tierId, request.grantedPlanId());
    validateExpression(request.expression(), 1);
    var expression = objectMapper.valueToTree(request.expression());
    MembershipTierCriteriaEntity configuredCriteria =
        membershipTierCriteriaRepository.findByMembershipTierId(tierId).orElse(null);
    rejectDuplicatePriority(request, configuredCriteria);
    MembershipTierCriteriaEntity saved = saveCriteria(tierId, request, expression, configuredCriteria);
    membershipTierCriteriaRepository.flush();
    return adminMembershipResponseMapper.toTierCriteria(saved);
  }

  private void requirePlanForTier(UUID tierId, UUID planId) {
    membershipTierRepository
        .findById(tierId)
        .orElseThrow(() -> new ResourceNotFoundException("Membership tier", tierId));
    var plan =
        membershipPlanRepository
            .findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership plan", planId));
    if (!plan.getMembershipTierId().equals(tierId)) {
      throw new ConflictException("The granted plan must belong to the membership tier being configured");
    }
  }

  private void rejectDuplicatePriority(
      UpsertMembershipTierCriteriaRequest request, MembershipTierCriteriaEntity configuredCriteria) {
    membershipTierCriteriaRepository
        .findByPriority(request.priority())
        .filter(other -> configuredCriteria == null || !other.getId().equals(configuredCriteria.getId()))
        .ifPresent(
            other -> {
              throw new ConflictException(
                  "Criteria priority " + request.priority() + " is already assigned to another tier");
            });
  }

  private MembershipTierCriteriaEntity saveCriteria(
      UUID tierId,
      UpsertMembershipTierCriteriaRequest request,
      com.fasterxml.jackson.databind.JsonNode expression,
      MembershipTierCriteriaEntity configuredCriteria) {
    if (configuredCriteria != null) {
      configuredCriteria.update(
          request.priority(), request.grantedPlanId(), request.evaluationWindow(), expression);
      configuredCriteria.recordUpdate(request.requestedBy());
      return configuredCriteria;
    }
    return createCriteria(tierId, request, expression);
  }

  private MembershipTierCriteriaEntity createCriteria(
      UUID tierId,
      UpsertMembershipTierCriteriaRequest request,
      com.fasterxml.jackson.databind.JsonNode expression) {
    MembershipTierCriteriaEntity criteria =
        new MembershipTierCriteriaEntity(
            tierId,
            request.priority(),
            request.grantedPlanId(),
            request.evaluationWindow(),
            expression);
    criteria.recordCreation(request.requestedBy());
    return membershipTierCriteriaRepository.save(criteria);
  }

  private void validateExpression(CriteriaExpressionRequest node, int depth) {
    if (depth > MAX_EXPRESSION_DEPTH)
      throw new ConflictException("Criteria expression exceeds the maximum nesting depth");
    boolean group = !node.children().isEmpty();
    if (group) {
      if (node.parameter() != null
          || node.threshold() != null
          || node.operator() == null
          || (node.operator() != CriteriaOperator.AND && node.operator() != CriteriaOperator.OR)) {
        throw new ConflictException(
            "A criteria group must use an AND or OR operator and contain only children");
      }
    }
    if (group) {
      node.children().forEach(child -> validateExpression(child, depth + 1));
      return;
    }
    if (node.parameter() == null || node.operator() == null || node.threshold() == null) {
      throw new ConflictException(
          "A criteria condition requires parameter, operator, and threshold");
    }
    switch (node.parameter()) {
      case ORDER_COUNT -> validateNumber(node, false);
      case ORDER_VALUE -> validateNumber(node, true);
      case COHORT -> validateCohort(node);
    }
  }

  private void validateNumber(CriteriaExpressionRequest node, boolean decimal) {
    if (node.operator() != CriteriaOperator.GREATER_THAN
        && node.operator() != CriteriaOperator.LESS_THAN)
      throw new ConflictException("Order criteria require GREATER_THAN or LESS_THAN");
    if (!node.threshold().isNumber()
        || node.threshold().decimalValue().compareTo(BigDecimal.ZERO) < 0
        || (!decimal && !node.threshold().canConvertToInt()))
      throw new ConflictException(
          decimal
              ? "ORDER_VALUE threshold must be a non-negative decimal"
              : "ORDER_COUNT threshold must be a non-negative integer");
  }

  private void validateCohort(CriteriaExpressionRequest node) {
    if (node.operator() != CriteriaOperator.IN_ANY && node.operator() != CriteriaOperator.IN_ALL
        || !node.threshold().isArray()
        || node.threshold().isEmpty())
      throw new ConflictException("COHORT requires IN_ANY or IN_ALL and a non-empty string list");
    Set<String> cohorts = new HashSet<>();
    node.threshold()
        .forEach(
            value -> {
              if (!value.isTextual() || value.asText().isBlank() || !cohorts.add(value.asText()))
                throw new ConflictException("COHORT values must be unique non-blank strings");
            });
  }
}
