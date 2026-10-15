package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.EvaluationWindow;
import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UpsertMembershipTierCriteriaRequest(
    @Positive int priority,
    @NotNull EvaluationWindow evaluationWindow,
    @NotNull UUID grantedPlanId,
    @NotNull @Valid CriteriaExpressionRequest expression,
    @NotBlank String requestedBy)
    implements AuditedRequest {}
