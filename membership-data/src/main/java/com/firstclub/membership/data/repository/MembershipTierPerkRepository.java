package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.entity.MembershipTierPerkEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface MembershipTierPerkRepository {
  Map<UUID, List<MembershipTierEntity>> findTiersByPerkIds(Set<UUID> membershipPerkIds);

  MembershipTierPerkEntity save(MembershipTierPerkEntity mapping);

  boolean exists(UUID tierId, UUID perkId);

  boolean delete(UUID tierId, UUID perkId);

  long countByPerkId(UUID perkId);
}
