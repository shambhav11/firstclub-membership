package com.firstclub.membership.models.request.user;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** The backend determines whether this is an upgrade or downgrade from the selected plan. */
public record ChangeMembershipPlanRequest(
    @NotNull UUID currentSubscriptionId,
    @NotNull UUID membershipPlanId,
    boolean autoRenew,
    @NotBlank String requestedBy)
    implements AuditedRequest {}
