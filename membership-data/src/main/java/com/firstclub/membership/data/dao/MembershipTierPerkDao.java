package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.entity.MembershipTierPerkEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipTierPerkDao {
  @PersistenceContext private EntityManager entityManager;

  public Map<UUID, List<MembershipTierEntity>> findTiersByPerkIds(Set<UUID> membershipPerkIds) {
    if (membershipPerkIds.isEmpty()) return Map.of();
    List<Object[]> rows =
        entityManager
            .createQuery(
                "select mapping.membershipPerkId, tier "
                    + "from MembershipTierPerkEntity mapping "
                    + "join MembershipTierEntity tier on tier.id = mapping.membershipTierId "
                    + "where mapping.membershipPerkId in :perkIds",
                Object[].class)
            .setParameter("perkIds", membershipPerkIds)
            .getResultList();
    Map<UUID, List<MembershipTierEntity>> result = new HashMap<>();
    for (Object[] row : rows) {
      result
          .computeIfAbsent((UUID) row[0], ignored -> new ArrayList<>())
          .add((MembershipTierEntity) row[1]);
    }
    return result;
  }

  public MembershipTierPerkEntity save(MembershipTierPerkEntity mapping) {
    entityManager.persist(mapping);
    return mapping;
  }

  public boolean exists(UUID tierId, UUID perkId) {
    return entityManager
            .createQuery(
                "select count(mapping) from MembershipTierPerkEntity mapping where"
                    + " mapping.membershipTierId = :tierId and mapping.membershipPerkId = :perkId",
                Long.class)
            .setParameter("tierId", tierId)
            .setParameter("perkId", perkId)
            .getSingleResult()
        > 0;
  }

  public boolean delete(UUID tierId, UUID perkId) {
    return entityManager
            .createQuery(
                "delete from MembershipTierPerkEntity mapping where mapping.membershipTierId ="
                    + " :tierId and mapping.membershipPerkId = :perkId")
            .setParameter("tierId", tierId)
            .setParameter("perkId", perkId)
            .executeUpdate()
        > 0;
  }

  public long countByPerkId(UUID perkId) {
    return entityManager
        .createQuery(
            "select count(mapping) from MembershipTierPerkEntity mapping where"
                + " mapping.membershipPerkId = :perkId",
            Long.class)
        .setParameter("perkId", perkId)
        .getSingleResult();
  }
}
