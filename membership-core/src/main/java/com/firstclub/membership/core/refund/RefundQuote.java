package com.firstclub.membership.core.refund;

import java.math.BigDecimal;

public record RefundQuote(int usedDays, int remainingDays, BigDecimal amount) {}
