package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.models.TierStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipTierDao {
  @PersistenceContext private EntityManager entityManager;

  public Optional<MembershipTierEntity> findById(UUID id) {
    return Optional.ofNullable(entityManager.find(MembershipTierEntity.class, id));
  }

  public List<MembershipTierEntity> findAllByIds(java.util.Set<UUID> ids) {
    if (ids.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select tier from MembershipTierEntity tier where tier.id in :ids",
            MembershipTierEntity.class)
        .setParameter("ids", ids)
        .getResultList();
  }

  public Optional<MembershipTierEntity> findByDisplayNameIgnoreCase(String displayName) {
    return entityManager
        .createQuery(
            "select tier from MembershipTierEntity tier where lower(tier.displayName) ="
                + " lower(:displayName)",
            MembershipTierEntity.class)
        .setParameter("displayName", displayName.trim())
        .getResultStream()
        .findFirst();
  }

  public MembershipTierEntity save(MembershipTierEntity tier) {
    entityManager.persist(tier);
    return tier;
  }

  public void flush() {
    entityManager.flush();
  }

  public Page<MembershipTierEntity> findPage(TierStatus status, Pageable pageable) {
    String predicate = status == null ? "" : " where tier.status = :status";
    var contentQuery =
        entityManager.createQuery(
            "select tier from MembershipTierEntity tier"
                + predicate
                + " order by tier.createdAt desc",
            MembershipTierEntity.class);
    var countQuery =
        entityManager.createQuery(
            "select count(tier) from MembershipTierEntity tier" + predicate, Long.class);
    if (status != null) {
      contentQuery.setParameter("status", status);
      countQuery.setParameter("status", status);
    }
    List<MembershipTierEntity> content =
        contentQuery
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    return new PageImpl<>(content, pageable, countQuery.getSingleResult());
  }

  /** Tiers that a customer may enroll in; administrative closed tiers remain hidden. */
  public List<MembershipTierEntity> findAllAvailableForEnrollment() {
    return entityManager
        .createQuery(
            "select tier from MembershipTierEntity tier "
                + "where tier.status = :status and tier.enrollmentClosed = false "
                + "order by tier.createdAt desc",
            MembershipTierEntity.class)
        .setParameter("status", TierStatus.ACTIVE)
        .getResultList();
  }
}
