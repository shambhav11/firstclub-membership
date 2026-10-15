package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record RemovePerksFromTierRequest(
    @NotEmpty List<@NotNull UUID> membershipPerkIds, @NotBlank String requestedBy)
    implements AuditedRequest {}
