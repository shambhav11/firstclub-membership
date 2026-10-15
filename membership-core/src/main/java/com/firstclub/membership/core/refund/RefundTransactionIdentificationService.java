package com.firstclub.membership.core.refund;

import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.core.query.PaymentFetchService;
import com.firstclub.membership.core.query.RefundAllocationFetchService;
import com.firstclub.membership.models.PaymentType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RefundTransactionIdentificationService {
  private final PaymentFetchService paymentFetchService;
  private final RefundAllocationFetchService refundAllocationFetchService;

  public RefundTransactionIdentificationService(
      PaymentFetchService paymentFetchService,
      RefundAllocationFetchService refundAllocationFetchService) {
    this.paymentFetchService = paymentFetchService;
    this.refundAllocationFetchService = refundAllocationFetchService;
  }

  public List<PaymentSourceAllocation> largestFirst(UUID subscriptionId, BigDecimal amount) {
    return allocate(candidatesFor(subscriptionId), amount, false).allocations();
  }

  /**
   * Splits replacement credit and surplus refund from one source pool. Credit is allocated first
   * so a downstream membership preserves the original payment lineage rather than assigning all
   * value to the newest direct payment.
   */
  public PlanChangePaymentSourceAllocations allocatePlanChange(
      UUID subscriptionId, BigDecimal creditAmount, BigDecimal refundAmount) {
    List<SourceCandidate> candidates = candidatesFor(subscriptionId);
    AllocationResult credit = allocate(candidates, creditAmount, true);
    AllocationResult refund = allocate(credit.remainingCandidates(), refundAmount, false);
    return new PlanChangePaymentSourceAllocations(credit.allocations(), refund.allocations());
  }

  private List<SourceCandidate> candidatesFor(UUID subscriptionId) {
    List<PaymentEntity> successfulPayments =
        paymentFetchService.findSuccessfulBySubscriptionId(subscriptionId);
    Map<UUID, PaymentEntity> sourcePayments = sourcePayments(successfulPayments);
    Map<UUID, BigDecimal> availableBySource = availableBySource(sourcePayments);
    return candidates(successfulPayments, availableBySource);
  }

  private Map<UUID, PaymentEntity> sourcePayments(List<PaymentEntity> successfulPayments) {
    Map<UUID, PaymentEntity> sourcePayments = creditSourcePayments(successfulPayments);
    successfulPayments.forEach(payment -> addSourcePayment(sourcePayments, payment));
    return sourcePayments;
  }

  private Map<UUID, PaymentEntity> creditSourcePayments(List<PaymentEntity> successfulPayments) {
    Set<UUID> sourceIds =
        successfulPayments.stream()
            .filter(payment -> payment.getPaymentType() == PaymentType.MEMBERSHIP_CREDIT)
            .map(PaymentEntity::getSourcePaymentId)
            .collect(java.util.stream.Collectors.toSet());
    Map<UUID, PaymentEntity> sourcePayments = new HashMap<>();
    paymentFetchService.findByIds(sourceIds).forEach(payment -> sourcePayments.put(payment.getId(), payment));
    return sourcePayments;
  }

  private void addSourcePayment(Map<UUID, PaymentEntity> sourcePayments, PaymentEntity payment) {
    UUID sourcePaymentId = sourcePaymentId(payment);
    if (payment.getPaymentType() == PaymentType.DIRECT_PAYMENT) {
      sourcePayments.put(sourcePaymentId, payment);
    }
    if (!sourcePayments.containsKey(sourcePaymentId)) {
      throw new IllegalStateException("Membership credit source payment is missing: " + sourcePaymentId);
    }
  }

  private Map<UUID, BigDecimal> availableBySource(Map<UUID, PaymentEntity> sourcePayments) {
    Map<UUID, BigDecimal> reservedBySource =
        refundAllocationFetchService.sumReservedBySourcePaymentIds(sourcePayments.keySet());
    Map<UUID, BigDecimal> availableBySource = new HashMap<>();
    sourcePayments.forEach(
        (sourceId, payment) -> availableBySource.put(sourceId, availableAmount(payment, reservedBySource)));
    return availableBySource;
  }

  private BigDecimal availableAmount(
      PaymentEntity payment, Map<UUID, BigDecimal> reservedBySource) {
    return payment
        .getAmount()
        .subtract(reservedBySource.getOrDefault(payment.getId(), BigDecimal.ZERO))
        .max(BigDecimal.ZERO);
  }

  private List<SourceCandidate> candidates(
      List<PaymentEntity> successfulPayments, Map<UUID, BigDecimal> availableBySource) {
    Map<UUID, SourceCandidate> candidates = new HashMap<>();
    for (PaymentEntity payment : successfulPayments) {
      UUID sourcePaymentId = sourcePaymentId(payment);
      SourceCandidate contribution =
          new SourceCandidate(
              sourcePaymentId,
              payment.getAmount().min(availableBySource.get(sourcePaymentId)),
              payment.getPaymentType() == PaymentType.MEMBERSHIP_CREDIT);
      candidates.merge(sourcePaymentId, contribution, SourceCandidate::combine);
    }
    return candidates.values().stream()
        .map(candidate -> candidate.limitTo(availableBySource.get(candidate.sourcePaymentId())))
        .toList();
  }

  private AllocationResult allocate(
      List<SourceCandidate> candidates, BigDecimal amount, boolean preserveCreditsFirst) {
    BigDecimal remaining = amount;
    List<PaymentSourceAllocation> allocations = new ArrayList<>();
    List<SourceCandidate> remainingCandidates = new ArrayList<>();
    for (SourceCandidate candidate : ordered(candidates, preserveCreditsFirst)) {
      BigDecimal selected = candidate.amount().min(remaining);
      if (selected.signum() > 0) {
        allocations.add(new PaymentSourceAllocation(candidate.sourcePaymentId(), selected));
        remaining = remaining.subtract(selected);
      }
      BigDecimal availableAfterAllocation = candidate.amount().subtract(selected);
      if (availableAfterAllocation.signum() > 0) {
        remainingCandidates.add(candidate.withAmount(availableAfterAllocation));
      }
    }
    if (remaining.signum() > 0) {
      throw new IllegalStateException("No sufficient refundable payment source is available");
    }
    return new AllocationResult(allocations, remainingCandidates);
  }

  private List<SourceCandidate> ordered(
      List<SourceCandidate> candidates, boolean preserveCreditsFirst) {
    return candidates.stream()
        .sorted(
            java.util.Comparator.comparing(
                    SourceCandidate::membershipCredit,
                    preserveCreditsFirst
                        ? java.util.Comparator.reverseOrder()
                        : java.util.Comparator.naturalOrder())
                .thenComparing(SourceCandidate::amount, java.util.Comparator.reverseOrder()))
        .toList();
  }

  private UUID sourcePaymentId(PaymentEntity payment) {
    return payment.getPaymentType() == PaymentType.DIRECT_PAYMENT
        ? payment.getId()
        : payment.getSourcePaymentId();
  }

  private record SourceCandidate(UUID sourcePaymentId, BigDecimal amount, boolean membershipCredit) {
    private SourceCandidate combine(SourceCandidate other) {
      return new SourceCandidate(
          sourcePaymentId, amount.add(other.amount), membershipCredit || other.membershipCredit);
    }

    private SourceCandidate limitTo(BigDecimal amountLimit) {
      return withAmount(amount.min(amountLimit));
    }

    private SourceCandidate withAmount(BigDecimal newAmount) {
      return new SourceCandidate(sourcePaymentId, newAmount, membershipCredit);
    }
  }

  private record AllocationResult(
      List<PaymentSourceAllocation> allocations, List<SourceCandidate> remainingCandidates) {}

  public record PlanChangePaymentSourceAllocations(
      List<PaymentSourceAllocation> creditAllocations,
      List<PaymentSourceAllocation> refundAllocations) {}
}
