package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipPerkDao {
  @PersistenceContext private EntityManager entityManager;

  public Page<MembershipPerkEntity> findPage(
      PerkStatus status,
      PerkAppliesTo appliesTo,
      String search,
      UUID unassignedToTierId,
      Pageable pageable) {
    String where = filters(status, appliesTo, search, unassignedToTierId);
    TypedQuery<MembershipPerkEntity> contentQuery = contentQuery(where);
    TypedQuery<Long> countQuery = countQuery(where);
    applyFilters(contentQuery, status, appliesTo, search, unassignedToTierId);
    applyFilters(countQuery, status, appliesTo, search, unassignedToTierId);
    List<MembershipPerkEntity> content = page(contentQuery, pageable);
    return new PageImpl<>(content, pageable, countQuery.getSingleResult());
  }

  private String filters(
      PerkStatus status, PerkAppliesTo appliesTo, String search, UUID unassignedToTierId) {
    StringBuilder where = new StringBuilder(" where 1 = 1");
    if (status != null) {
      where.append(" and perk.status = :status");
    }
    if (appliesTo != null) {
      where.append(" and perk.appliesTo = :appliesTo");
    }
    if (search != null && !search.isBlank()) {
      where.append(" and perk.displayTextKey like :search");
    }
    if (unassignedToTierId != null) {
      where.append(unassignedTierFilter());
    }
    return where.toString();
  }

  private String unassignedTierFilter() {
    return " and perk.id not in (select mapping.membershipPerkId from MembershipTierPerkEntity"
        + " mapping where mapping.membershipTierId = :tierId)";
  }

  private TypedQuery<MembershipPerkEntity> contentQuery(String where) {
    return entityManager.createQuery(
        "select perk from MembershipPerkEntity perk" + where + " order by perk.createdAt desc",
        MembershipPerkEntity.class);
  }

  private TypedQuery<Long> countQuery(String where) {
    return entityManager.createQuery("select count(perk) from MembershipPerkEntity perk" + where, Long.class);
  }

  private List<MembershipPerkEntity> page(
      TypedQuery<MembershipPerkEntity> query, Pageable pageable) {
    return query
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  public Page<MembershipPerkEntity> findByMembershipTierId(
      UUID membershipTierId, Pageable pageable) {
    String where =
        " where perk.id in (select mapping.membershipPerkId from MembershipTierPerkEntity mapping"
            + " where mapping.membershipTierId = :tierId)";
    List<MembershipPerkEntity> content =
        entityManager
            .createQuery(
                "select perk from MembershipPerkEntity perk"
                    + where
                    + " order by perk.createdAt desc",
                MembershipPerkEntity.class)
            .setParameter("tierId", membershipTierId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    Long total =
        entityManager
            .createQuery("select count(perk) from MembershipPerkEntity perk" + where, Long.class)
            .setParameter("tierId", membershipTierId)
            .getSingleResult();
    return new PageImpl<>(content, pageable, total);
  }

  public List<MembershipPerkEntity> findAllByMembershipTierId(UUID membershipTierId) {
    return entityManager
        .createQuery(
            "select perk from MembershipPerkEntity perk where perk.id in (select"
                + " mapping.membershipPerkId from MembershipTierPerkEntity mapping where"
                + " mapping.membershipTierId = :tierId) order by perk.createdAt desc",
            MembershipPerkEntity.class)
        .setParameter("tierId", membershipTierId)
        .getResultList();
  }

  public java.util.Map<UUID, List<MembershipPerkEntity>> findActiveByMembershipTierIds(
      java.util.Set<UUID> membershipTierIds) {
    if (membershipTierIds.isEmpty()) return java.util.Map.of();
    List<Object[]> rows =
        entityManager
            .createQuery(
                "select mapping.membershipTierId, perk from MembershipTierPerkEntity mapping"
                    + " join MembershipPerkEntity perk on perk.id = mapping.membershipPerkId"
                    + " where mapping.membershipTierId in :tierIds and perk.status = :status"
                    + " order by perk.createdAt desc",
                Object[].class)
            .setParameter("tierIds", membershipTierIds)
            .setParameter("status", PerkStatus.ACTIVE)
            .getResultList();
    java.util.Map<UUID, List<MembershipPerkEntity>> perksByTier = new java.util.HashMap<>();
    for (Object[] row : rows) {
      perksByTier
          .computeIfAbsent((UUID) row[0], ignored -> new java.util.ArrayList<>())
          .add((MembershipPerkEntity) row[1]);
    }
    return perksByTier;
  }

  public Optional<MembershipPerkEntity> findActiveByAppliesToAndDisplayTextKey(
      PerkAppliesTo appliesTo, String key) {
    return entityManager
        .createQuery(
            "select perk from MembershipPerkEntity perk where perk.appliesTo = :appliesTo and"
                + " perk.displayTextKey = :key and perk.status = :status",
            MembershipPerkEntity.class)
        .setParameter("appliesTo", appliesTo)
        .setParameter("key", key)
        .setParameter("status", PerkStatus.ACTIVE)
        .getResultStream()
        .findFirst();
  }

  public Optional<MembershipPerkEntity> findById(UUID id) {
    return Optional.ofNullable(entityManager.find(MembershipPerkEntity.class, id));
  }

  public MembershipPerkEntity save(MembershipPerkEntity perk) {
    entityManager.persist(perk);
    return perk;
  }

  private void applyFilters(
      jakarta.persistence.Query query,
      PerkStatus status,
      PerkAppliesTo appliesTo,
      String search,
      UUID tierId) {
    if (status != null) query.setParameter("status", status);
    if (appliesTo != null) query.setParameter("appliesTo", appliesTo);
    if (search != null && !search.isBlank())
      query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
    if (tierId != null) query.setParameter("tierId", tierId);
  }
}
