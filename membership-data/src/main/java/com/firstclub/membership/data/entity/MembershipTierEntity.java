package com.firstclub.membership.data.entity;

import com.firstclub.membership.models.TierStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "membership_tiers")
public class MembershipTierEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TierStatus status;

  @Column(name = "enrollment_closed", nullable = false)
  private boolean enrollmentClosed;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MembershipTierEntity() {}

  public MembershipTierEntity(String displayName, TierStatus status, boolean enrollmentClosed) {
    this.displayName = displayName;
    this.status = status;
    this.enrollmentClosed = enrollmentClosed;
  }

  public UUID getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public TierStatus getStatus() {
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

  public void update(String displayName, TierStatus status, boolean enrollmentClosed) {
    this.displayName = displayName;
    this.status = status;
    this.enrollmentClosed = enrollmentClosed;
  }
}
