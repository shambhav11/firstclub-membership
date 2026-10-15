package com.firstclub.membership.data.entity;

import com.firstclub.membership.models.SubscriptionOrigin;
import com.firstclub.membership.models.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "membership_subscriptions")
public class MembershipSubscriptionEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "membership_plan_id", nullable = false)
  private UUID membershipPlanId;

  @Column(name = "activation_timestamp")
  private Instant activationTimestamp;

  @Column(name = "expiry_timestamp")
  private Instant expiryTimestamp;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
  private BigDecimal amountPaid;

  @Column(name = "membership_value_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal membershipValueAmount;

  @Column(name = "credit_applied_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal creditAppliedAmount;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "auto_renew", nullable = false)
  private boolean autoRenew;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false)
  private SubscriptionOrigin origin;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MembershipSubscriptionEntity() {}

  public MembershipSubscriptionEntity(
      UUID userId,
      UUID membershipPlanId,
      BigDecimal membershipValueAmount,
      BigDecimal creditAppliedAmount,
      String currencyCode,
      boolean autoRenew) {
    this.userId = userId;
    this.membershipPlanId = membershipPlanId;
    this.membershipValueAmount = membershipValueAmount;
    this.creditAppliedAmount = creditAppliedAmount;
    this.amountPaid = BigDecimal.ZERO;
    this.currencyCode = currencyCode;
    this.autoRenew = autoRenew;
    this.status = SubscriptionStatus.PENDING_PAYMENT;
    this.origin = SubscriptionOrigin.PURCHASED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getMembershipPlanId() {
    return membershipPlanId;
  }

  public BigDecimal getAmountPaid() {
    return amountPaid;
  }

  public BigDecimal getMembershipValueAmount() {
    return membershipValueAmount;
  }

  public BigDecimal getCreditAppliedAmount() {
    return creditAppliedAmount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public boolean isAutoRenew() {
    return autoRenew;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public SubscriptionOrigin getOrigin() {
    return origin;
  }

  public Instant getActivationTimestamp() {
    return activationTimestamp;
  }

  public Instant getExpiryTimestamp() {
    return expiryTimestamp;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void activate(
      Instant activationTimestamp, Instant expiryTimestamp, BigDecimal amountPaid) {
    this.activationTimestamp = activationTimestamp;
    this.expiryTimestamp = expiryTimestamp;
    this.amountPaid = amountPaid;
    this.status = SubscriptionStatus.ACTIVE;
  }

  public void cancel(Instant cancelledAt) {
    this.cancelledAt = cancelledAt;
    this.status = SubscriptionStatus.CANCELLED;
    this.autoRenew = false;
  }

  public void expire() {
    this.status = SubscriptionStatus.EXPIRED;
    this.autoRenew = false;
  }

  public void extendExpiry(int durationDays) {
    this.expiryTimestamp = this.expiryTimestamp.plus(java.time.Duration.ofDays(durationDays));
  }

  public void addAmountPaid(BigDecimal amount) {
    this.amountPaid = this.amountPaid.add(amount);
  }

  public static MembershipSubscriptionEntity criteriaGranted(
      UUID userId, UUID membershipPlanId, String currencyCode, Instant activation, Instant expiry) {
    MembershipSubscriptionEntity subscription =
        new MembershipSubscriptionEntity(
            userId, membershipPlanId, BigDecimal.ZERO, BigDecimal.ZERO, currencyCode, false);
    subscription.origin = SubscriptionOrigin.CRITERIA_GRANTED;
    subscription.activate(activation, expiry, BigDecimal.ZERO);
    return subscription;
  }
}
