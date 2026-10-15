package com.firstclub.membership.models.response.user;

import com.firstclub.membership.models.CriteriaEvaluationOutcome;
import java.util.UUID;

/** Result of evaluating the configured tier criteria for one user. */
public record CriteriaEvaluationResponse(
    CriteriaEvaluationOutcome outcome,
    UUID matchedTierId,
    String matchedTierDisplayName,
    UUID grantedPlanId,
    MembershipSubscriptionResponse membership) {}
