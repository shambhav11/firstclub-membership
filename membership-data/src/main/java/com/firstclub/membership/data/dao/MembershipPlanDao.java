package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.models.PlanStatus;
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
public class MembershipPlanDao {
  @PersistenceContext private EntityManager entityManager;

  public Page<MembershipPlanEntity> findByMembershipTierId(
      UUID membershipTierId, Pageable pageable) {
    String where = " where plan.membershipTierId = :membershipTierId";
    List<MembershipPlanEntity> content =
        entityManager
            .createQuery(
                "select plan from MembershipPlanEntity plan"
                    + where
                    + " order by plan.durationDays asc",
                MembershipPlanEntity.class)
            .setParameter("membershipTierId", membershipTierId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    Long total =
        entityManager
            .createQuery("select count(plan) from MembershipPlanEntity plan" + where, Long.class)
            .setParameter("membershipTierId", membershipTierId)
            .getSingleResult();
    return new PageImpl<>(content, pageable, total);
  }

  public List<MembershipPlanEntity> findAllByMembershipTierId(UUID membershipTierId) {
    return entityManager
        .createQuery(
            "select plan from MembershipPlanEntity plan where plan.membershipTierId ="
                + " :membershipTierId order by plan.durationDays asc",
            MembershipPlanEntity.class)
        .setParameter("membershipTierId", membershipTierId)
        .getResultList();
  }

  public List<MembershipPlanEntity> findAllByIds(java.util.Set<UUID> ids) {
    if (ids.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select plan from MembershipPlanEntity plan where plan.id in :ids",
            MembershipPlanEntity.class)
        .setParameter("ids", ids)
        .getResultList();
  }

  public List<MembershipPlanEntity> findAvailableByMembershipTierIds(java.util.Set<UUID> tierIds) {
    if (tierIds.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select plan from MembershipPlanEntity plan where plan.membershipTierId in :tierIds "
                + "and plan.status = :status and plan.enrollmentClosed = false "
                + "order by plan.durationDays asc",
            MembershipPlanEntity.class)
        .setParameter("tierIds", tierIds)
        .setParameter("status", PlanStatus.ACTIVE)
        .getResultList();
  }

  public MembershipPlanEntity save(MembershipPlanEntity plan) {
    entityManager.persist(plan);
    return plan;
  }

  public Optional<MembershipPlanEntity> findById(UUID id) {
    return Optional.ofNullable(entityManager.find(MembershipPlanEntity.class, id));
  }
}
