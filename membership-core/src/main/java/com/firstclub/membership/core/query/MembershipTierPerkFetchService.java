package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.repository.MembershipTierPerkRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused tier-perk mapping reads backed only by the mapping repository. */
@Service
@Transactional(readOnly = true)
public class MembershipTierPerkFetchService {
  private final MembershipTierPerkRepository membershipTierPerkRepository;

  public MembershipTierPerkFetchService(MembershipTierPerkRepository membershipTierPerkRepository) {
    this.membershipTierPerkRepository = membershipTierPerkRepository;
  }

  public Map<UUID, List<MembershipTierEntity>> findTiersByPerkIds(Set<UUID> membershipPerkIds) {
    return membershipTierPerkRepository.findTiersByPerkIds(membershipPerkIds);
  }
}
