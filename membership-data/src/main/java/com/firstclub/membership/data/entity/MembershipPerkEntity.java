package com.firstclub.membership.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "membership_perks")
public class MembershipPerkEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "display_text", nullable = false)
  private JsonNode displayText;

  @Column(name = "display_text_key", nullable = false)
  private String displayTextKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "applies_to", nullable = false)
  private PerkAppliesTo appliesTo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "perk_application", nullable = false)
  private JsonNode perkApplication;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PerkStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MembershipPerkEntity() {}

  public MembershipPerkEntity(
      JsonNode displayText,
      String displayTextKey,
      PerkAppliesTo appliesTo,
      JsonNode perkApplication,
      PerkStatus status) {
    this.displayText = displayText;
    this.displayTextKey = displayTextKey;
    this.appliesTo = appliesTo;
    this.perkApplication = perkApplication;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public JsonNode getDisplayText() {
    return displayText;
  }

  public String getDisplayTextKey() {
    return displayTextKey;
  }

  public PerkAppliesTo getAppliesTo() {
    return appliesTo;
  }

  public JsonNode getPerkApplication() {
    return perkApplication;
  }

  public PerkStatus getStatus() {
    return status;
  }

  public void archive() {
    this.status = PerkStatus.ARCHIVED;
  }

  /** Restores an archived configuration without changing its immutable perk definition. */
  public void reactivate() {
    this.status = PerkStatus.ACTIVE;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
