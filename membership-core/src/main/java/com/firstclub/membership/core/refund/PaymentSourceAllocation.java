package com.firstclub.membership.core.refund;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSourceAllocation(UUID sourcePaymentId, BigDecimal amount) {}
