package com.firstclub.membership.models.request.testing;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Assignment-only stand-in for the order and user-service facts consumed by criteria evaluation.
 */
public record SetCriteriaFactsRequest(
    @PositiveOrZero int orderCount,
    @NotNull @PositiveOrZero BigDecimal orderValue,
    Set<String> cohorts,
    @NotBlank String requestedBy)
    implements AuditedRequest {
  public SetCriteriaFactsRequest {
    cohorts = cohorts == null ? Set.of() : Set.copyOf(cohorts);
  }
}
