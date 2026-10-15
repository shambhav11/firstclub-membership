package com.firstclub.membership.core.query;

import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.TierStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused tier reads backed only by the tier repository. */
@Service
@Transactional(readOnly = true)
public class MembershipTierFetchService {
  private final MembershipTierRepository membershipTierRepository;

  public MembershipTierFetchService(MembershipTierRepository membershipTierRepository) {
    this.membershipTierRepository = membershipTierRepository;
  }

  public MembershipTierEntity getRequired(UUID membershipTierId) {
    return membershipTierRepository
        .findById(membershipTierId)
        .orElseThrow(() -> new ResourceNotFoundException("Membership tier", membershipTierId));
  }

  public Page<MembershipTierEntity> getPage(TierStatus status, Pageable pageable) {
    return membershipTierRepository.findPage(status, pageable);
  }

  public List<MembershipTierEntity> getAvailableForEnrollment() {
    return membershipTierRepository.findAllAvailableForEnrollment();
  }

  public Map<UUID, MembershipTierEntity> getByIds(Set<UUID> ids) {
    return membershipTierRepository.findAllByIds(ids).stream()
        .collect(java.util.stream.Collectors.toMap(MembershipTierEntity::getId, tier -> tier));
  }

  public List<MembershipTierEntity> findAllByIds(Set<UUID> ids) {
    return membershipTierRepository.findAllByIds(ids);
  }

  public Optional<MembershipTierEntity> findByDisplayNameIgnoreCase(String displayName) {
    return membershipTierRepository.findByDisplayNameIgnoreCase(displayName);
  }
}
