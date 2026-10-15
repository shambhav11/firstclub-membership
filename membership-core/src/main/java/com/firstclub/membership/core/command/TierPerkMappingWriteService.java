package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipTierPerkEntity;
import com.firstclub.membership.data.repository.MembershipPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.PerkStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TierPerkMappingWriteService {
  private final MembershipTierRepository membershipTierRepository;
  private final MembershipPerkRepository membershipPerkRepository;
  private final MembershipTierPerkRepository membershipTierPerkRepository;

  public TierPerkMappingWriteService(
      MembershipTierRepository membershipTierRepository,
      MembershipPerkRepository membershipPerkRepository,
      MembershipTierPerkRepository membershipTierPerkRepository) {
    this.membershipTierRepository = membershipTierRepository;
    this.membershipPerkRepository = membershipPerkRepository;
    this.membershipTierPerkRepository = membershipTierPerkRepository;
  }

  @Transactional
  public void add(UUID tierId, List<UUID> perkIds, String actor) {
    requireTier(tierId);
    for (UUID perkId : perkIds.stream().distinct().toList()) {
      var perk = requirePerk(perkId);
      if (perk.getStatus() != PerkStatus.ACTIVE)
        throw new ConflictException("Archived perks cannot be added to a tier");
      if (!membershipTierPerkRepository.exists(tierId, perkId))
        {
          MembershipTierPerkEntity mapping = new MembershipTierPerkEntity(tierId, perkId);
          mapping.recordCreation(actor);
          membershipTierPerkRepository.save(mapping);
        }
    }
    membershipTierRepository.flush();
  }

  @Transactional
  public void remove(UUID tierId, List<UUID> perkIds, String actor) {
    requireTier(tierId);
    for (UUID perkId : perkIds.stream().distinct().toList()) {
      var perk = requirePerk(perkId);
      boolean removed = membershipTierPerkRepository.delete(tierId, perkId);
      if (removed && membershipTierPerkRepository.countByPerkId(perkId) == 0) {
        perk.archive();
        perk.recordUpdate(actor);
      }
    }
    membershipTierRepository.flush();
  }

  private void requireTier(UUID id) {
    if (membershipTierRepository.findById(id).isEmpty())
      throw new ResourceNotFoundException("Membership tier", id);
  }

  private com.firstclub.membership.data.entity.MembershipPerkEntity requirePerk(UUID id) {
    return membershipPerkRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Membership perk", id));
  }
}
