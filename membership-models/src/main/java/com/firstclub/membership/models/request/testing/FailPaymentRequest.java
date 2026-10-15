package com.firstclub.membership.models.request.testing;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Test-only replacement for a failed payment-gateway webhook. */
public record FailPaymentRequest(
    @NotBlank @Size(max = 500) String reason, @NotBlank String requestedBy)
    implements AuditedRequest {}
