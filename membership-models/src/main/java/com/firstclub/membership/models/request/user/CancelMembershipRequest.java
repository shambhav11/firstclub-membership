package com.firstclub.membership.models.request.user;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cancellation is immediate; a reason is optional but retained for audit and support. */
public record CancelMembershipRequest(@Size(max = 500) String reason, @NotBlank String requestedBy)
    implements AuditedRequest {}
