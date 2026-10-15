package com.firstclub.membership.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/** Internal audit metadata. It is intentionally excluded from every API response mapper. */
@MappedSuperclass
public abstract class AuditableEntity {
  public static final String SYSTEM_ACTOR = "SYSTEM";

  @Column(
      name = "created_by",
      nullable = false,
      updatable = false,
      columnDefinition = "TEXT DEFAULT 'SYSTEM'")
  private String createdBy = SYSTEM_ACTOR;

  @Column(name = "updated_by", nullable = false, columnDefinition = "TEXT DEFAULT 'SYSTEM'")
  private String updatedBy = SYSTEM_ACTOR;

  public void recordCreation(String actor) {
    createdBy = normalizeActor(actor);
    updatedBy = createdBy;
  }

  public void recordUpdate(String actor) {
    updatedBy = normalizeActor(actor);
  }

  private static String normalizeActor(String actor) {
    return actor == null || actor.isBlank() ? SYSTEM_ACTOR : actor.trim();
  }

  @JsonIgnore
  public String getCreatedBy() {
    return createdBy;
  }

  @JsonIgnore
  public String getUpdatedBy() {
    return updatedBy;
  }
}
