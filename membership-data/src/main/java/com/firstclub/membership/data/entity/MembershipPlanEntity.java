package com.firstclub.membership.data.entity;

import com.firstclub.membership.models.PlanStatus;
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
@Table(name = "membership_plans")
public class MembershipPlanEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "membership_tier_id", nullable = false)
  private UUID membershipTierId;

  @Column(name = "duration_days", nullable = false)
  private int durationDays;

  @Column(name = "display_text", nullable = false)
  private String displayText;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlanStatus status;

  @Column(name = "enrollment_closed", nullable = false)
  private boolean enrollmentClosed;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MembershipPlanEntity() {}

  public MembershipPlanEntity(
      UUID membershipTierId,
      int durationDays,
      String displayText,
      BigDecimal amount,
      String currencyCode,
      PlanStatus status,
      boolean enrollmentClosed) {
    this.membershipTierId = membershipTierId;
    this.durationDays = durationDays;
    this.displayText = displayText;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.status = status;
    this.enrollmentClosed = enrollmentClosed;
  }

  public UUID getId() {
    return id;
  }

  public UUID getMembershipTierId() {
    return membershipTierId;
  }

  public int getDurationDays() {
    return durationDays;
  }

  public String getDisplayText() {
    return displayText;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public PlanStatus getStatus() {
    return status;
  }

  public boolean isEnrollmentClosed() {
    return enrollmentClosed;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(
      String displayText,
      BigDecimal amount,
      String currencyCode,
      PlanStatus status,
      boolean enrollmentClosed) {
    this.displayText = displayText;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.status = status;
    this.enrollmentClosed = enrollmentClosed;
  }
}
