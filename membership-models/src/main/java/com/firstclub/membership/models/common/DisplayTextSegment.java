package com.firstclub.membership.models.common;

import jakarta.validation.constraints.NotBlank;

public record DisplayTextSegment(@NotBlank String text, boolean bold) {}
