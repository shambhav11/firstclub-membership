package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipPerkRequest(@NotNull PerkStatus status, @NotBlank String requestedBy)
    implements AuditedRequest {}
