package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipTierRequest(
    @NotBlank String displayName,
    @NotNull TierStatus status,
    boolean enrollmentClosed,
    @NotBlank String requestedBy)
    implements AuditedRequest {}
