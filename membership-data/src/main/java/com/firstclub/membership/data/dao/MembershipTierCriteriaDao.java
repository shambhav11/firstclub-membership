package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipTierCriteriaDao {
  @PersistenceContext private EntityManager entityManager;

  public Optional<MembershipTierCriteriaEntity> findByMembershipTierId(UUID membershipTierId) {
    return entityManager
        .createQuery(
            "select criteria from MembershipTierCriteriaEntity criteria where"
                + " criteria.membershipTierId = :membershipTierId",
            MembershipTierCriteriaEntity.class)
        .setParameter("membershipTierId", membershipTierId)
        .getResultStream()
        .findFirst();
  }

  public Optional<MembershipTierCriteriaEntity> findByPriority(int priority) {
    return entityManager
        .createQuery(
            "select criteria from MembershipTierCriteriaEntity criteria where criteria.priority ="
                + " :priority",
            MembershipTierCriteriaEntity.class)
        .setParameter("priority", priority)
        .getResultStream()
        .findFirst();
  }

  public List<MembershipTierCriteriaEntity> findAllOrderedByPriority() {
    return entityManager
        .createQuery(
            "select criteria from MembershipTierCriteriaEntity criteria order by criteria.priority"
                + " asc, criteria.createdAt asc",
            MembershipTierCriteriaEntity.class)
        .getResultList();
  }

  public MembershipTierCriteriaEntity save(MembershipTierCriteriaEntity criteria) {
    entityManager.persist(criteria);
    return criteria;
  }

  public void flush() {
    entityManager.flush();
  }
}
