package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateMembershipTierRequest(
    @NotBlank String displayName,
    boolean enrollmentClosed,
    @NotEmpty List<@Valid InitialMembershipPlanRequest> initialPlans,
    List<@Valid NewMembershipPerkRequest> perks,
    @NotBlank String requestedBy)
    implements AuditedRequest {
  public CreateMembershipTierRequest {
    perks = perks == null ? List.of() : List.copyOf(perks);
  }
}
