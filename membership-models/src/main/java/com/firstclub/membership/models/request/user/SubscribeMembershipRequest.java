package com.firstclub.membership.models.request.user;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** A plan uniquely identifies its membership tier, so a tier id is not accepted separately. */
public record SubscribeMembershipRequest(
    @NotNull UUID membershipPlanId, boolean autoRenew, @NotBlank String requestedBy)
    implements AuditedRequest {}
