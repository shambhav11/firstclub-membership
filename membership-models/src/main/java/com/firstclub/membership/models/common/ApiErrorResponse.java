package com.firstclub.membership.models.common;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    String code, String message, List<FieldViolationResponse> fieldViolations, Instant timestamp) {}
