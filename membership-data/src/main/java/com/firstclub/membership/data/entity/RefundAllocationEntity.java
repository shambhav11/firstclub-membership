package com.firstclub.membership.data.entity;

import com.firstclub.membership.models.RefundStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "refund_allocations")
public class RefundAllocationEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "refund_id", nullable = false)
  private UUID refundId;

  @Column(name = "source_payment_id", nullable = false)
  private UUID sourcePaymentId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RefundStatus status;

  @Column(name = "external_reference")
  private String externalReference;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_attempted_at")
  private Instant lastAttemptedAt;

  @Column(name = "failure_reason")
  private String failureReason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected RefundAllocationEntity() {}

  public RefundAllocationEntity(
      UUID refundId,
      UUID sourcePaymentId,
      BigDecimal amount,
      String currencyCode,
      RefundStatus status) {
    this.refundId = refundId;
    this.sourcePaymentId = sourcePaymentId;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRefundId() {
    return refundId;
  }

  public UUID getSourcePaymentId() {
    return sourcePaymentId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public RefundStatus getStatus() {
    return status;
  }

  public void complete(String externalReference) {
    this.status = RefundStatus.COMPLETED;
    this.externalReference = externalReference;
    this.failureReason = null;
    this.attemptCount++;
    this.lastAttemptedAt = Instant.now();
  }

  public void fail(String reason) {
    this.status = RefundStatus.FAILED;
    this.failureReason = reason;
    this.attemptCount++;
    this.lastAttemptedAt = Instant.now();
  }
}
