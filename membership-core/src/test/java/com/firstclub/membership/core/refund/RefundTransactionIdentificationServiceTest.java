package com.firstclub.membership.core.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firstclub.membership.core.query.PaymentFetchService;
import com.firstclub.membership.core.query.RefundAllocationFetchService;
import com.firstclub.membership.data.entity.PaymentEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefundTransactionIdentificationServiceTest {
  private final PaymentFetchService payments = mock(PaymentFetchService.class);
  private final RefundAllocationFetchService refundAllocations = mock(RefundAllocationFetchService.class);
  private final RefundTransactionIdentificationService service =
      new RefundTransactionIdentificationService(payments, refundAllocations);

  @Test
  void preservesSplitPaymentLineageAcrossAPlanChangeAndLaterCancellation() {
    UUID goldSubscriptionId = UUID.randomUUID();
    UUID platinumSubscriptionId = UUID.randomUUID();
    UUID goldQuarterlySubscriptionId = UUID.randomUUID();
    PaymentEntity goldDirect = directPayment(goldSubscriptionId, "500.00");
    PaymentEntity platinumDirect = directPayment(platinumSubscriptionId, "2500.00");
    PaymentEntity platinumCredit =
        creditPayment(platinumSubscriptionId, goldSubscriptionId, goldDirect.getId(), "500.00");

    when(payments.findSuccessfulBySubscriptionId(platinumSubscriptionId))
        .thenReturn(List.of(platinumCredit, platinumDirect));
    when(payments.findByIds(anySet())).thenReturn(List.of(goldDirect));
    when(refundAllocations.sumReservedBySourcePaymentIds(anySet())).thenReturn(Map.of());

    var planChange =
        service.allocatePlanChange(
            platinumSubscriptionId, new BigDecimal("1000.00"), new BigDecimal("2000.00"));

    assertEquals(
        Map.of(goldDirect.getId(), new BigDecimal("500.00"), platinumDirect.getId(), new BigDecimal("500.00")),
        amountsBySource(planChange.creditAllocations()));
    assertEquals(
        Map.of(platinumDirect.getId(), new BigDecimal("2000.00")),
        amountsBySource(planChange.refundAllocations()));

    PaymentEntity goldQuarterlyGoldCredit =
        creditPayment(
            goldQuarterlySubscriptionId, platinumSubscriptionId, goldDirect.getId(), "500.00");
    PaymentEntity goldQuarterlyPlatinumCredit =
        creditPayment(
            goldQuarterlySubscriptionId, platinumSubscriptionId, platinumDirect.getId(), "500.00");
    when(payments.findSuccessfulBySubscriptionId(goldQuarterlySubscriptionId))
        .thenReturn(List.of(goldQuarterlyGoldCredit, goldQuarterlyPlatinumCredit));
    when(payments.findByIds(anySet())).thenReturn(List.of(goldDirect, platinumDirect));
    when(refundAllocations.sumReservedBySourcePaymentIds(anySet()))
        .thenReturn(Map.of(platinumDirect.getId(), new BigDecimal("2000.00")));

    List<PaymentSourceAllocation> cancellation =
        service.largestFirst(goldQuarterlySubscriptionId, new BigDecimal("1000.00"));

    assertEquals(
        Map.of(goldDirect.getId(), new BigDecimal("500.00"), platinumDirect.getId(), new BigDecimal("500.00")),
        amountsBySource(cancellation));
    verify(payments, never()).findById(org.mockito.ArgumentMatchers.any());
  }

  private PaymentEntity directPayment(UUID subscriptionId, String amount) {
    PaymentEntity payment =
        new PaymentEntity(
            subscriptionId,
            UUID.randomUUID().toString(),
            new BigDecimal(amount),
            "INR",
            Instant.now().plusSeconds(900));
    ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
    payment.complete("bank-reference");
    return payment;
  }

  private PaymentEntity creditPayment(
      UUID subscriptionId, UUID sourceSubscriptionId, UUID sourcePaymentId, String amount) {
    PaymentEntity payment =
        PaymentEntity.membershipCredit(
            subscriptionId,
            new BigDecimal(amount),
            "INR",
            sourceSubscriptionId,
            sourcePaymentId);
    ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
    return payment;
  }

  private Map<UUID, BigDecimal> amountsBySource(List<PaymentSourceAllocation> allocations) {
    return allocations.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                PaymentSourceAllocation::sourcePaymentId, PaymentSourceAllocation::amount));
  }
}
