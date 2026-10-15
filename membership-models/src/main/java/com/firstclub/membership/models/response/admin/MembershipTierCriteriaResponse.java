package com.firstclub.membership.models.response.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.firstclub.membership.models.EvaluationWindow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipTierCriteriaResponse(
    UUID id,
    UUID membershipTierId,
    int priority,
    UUID grantedPlanId,
    EvaluationWindow evaluationWindow,
    JsonNode expression,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
