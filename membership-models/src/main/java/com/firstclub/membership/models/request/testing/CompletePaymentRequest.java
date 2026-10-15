package com.firstclub.membership.models.request.testing;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;

/** Test-only replacement for a successful payment-gateway webhook. */
public record CompletePaymentRequest(@NotBlank String bankReferenceId, @NotBlank String requestedBy)
    implements AuditedRequest {}
