package com.firstclub.membership.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "membership_tier_perks")
public class MembershipTierPerkEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "membership_tier_id", nullable = false)
  private UUID membershipTierId;

  @Column(name = "membership_perk_id", nullable = false)
  private UUID membershipPerkId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected MembershipTierPerkEntity() {}

  public MembershipTierPerkEntity(UUID membershipTierId, UUID membershipPerkId) {
    this.membershipTierId = membershipTierId;
    this.membershipPerkId = membershipPerkId;
  }

  public UUID getMembershipTierId() {
    return membershipTierId;
  }

  public UUID getMembershipPerkId() {
    return membershipPerkId;
  }
}
