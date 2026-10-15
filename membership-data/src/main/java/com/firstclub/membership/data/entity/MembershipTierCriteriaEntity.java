package com.firstclub.membership.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.firstclub.membership.models.EvaluationWindow;
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
@Table(name = "membership_tier_criteria")
public class MembershipTierCriteriaEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "membership_tier_id", nullable = false)
  private UUID membershipTierId;

  @Column(nullable = false)
  private int priority;

  @Column(name = "granted_plan_id", nullable = false)
  private UUID grantedPlanId;

  @Enumerated(EnumType.STRING)
  @Column(name = "evaluation_window", nullable = false)
  private EvaluationWindow evaluationWindow;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private JsonNode expression;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MembershipTierCriteriaEntity() {}

  public MembershipTierCriteriaEntity(
      UUID membershipTierId,
      int priority,
      UUID grantedPlanId,
      EvaluationWindow evaluationWindow,
      JsonNode expression) {
    this.membershipTierId = membershipTierId;
    this.priority = priority;
    this.grantedPlanId = grantedPlanId;
    this.evaluationWindow = evaluationWindow;
    this.expression = expression;
  }

  public UUID getId() {
    return id;
  }

  public UUID getMembershipTierId() {
    return membershipTierId;
  }

  public int getPriority() {
    return priority;
  }

  public UUID getGrantedPlanId() {
    return grantedPlanId;
  }

  public EvaluationWindow getEvaluationWindow() {
    return evaluationWindow;
  }

  public JsonNode getExpression() {
    return expression;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(
      int priority, UUID grantedPlanId, EvaluationWindow evaluationWindow, JsonNode expression) {
    this.priority = priority;
    this.grantedPlanId = grantedPlanId;
    this.evaluationWindow = evaluationWindow;
    this.expression = expression;
  }
}
