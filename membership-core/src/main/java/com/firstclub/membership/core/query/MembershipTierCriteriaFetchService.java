package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import com.firstclub.membership.data.repository.MembershipTierCriteriaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused criteria reads backed only by the tier-criteria repository. */
@Service
@Transactional(readOnly = true)
public class MembershipTierCriteriaFetchService {
  private final MembershipTierCriteriaRepository membershipTierCriteriaRepository;

  public MembershipTierCriteriaFetchService(
      MembershipTierCriteriaRepository membershipTierCriteriaRepository) {
    this.membershipTierCriteriaRepository = membershipTierCriteriaRepository;
  }

  public Optional<MembershipTierCriteriaEntity> findByTierId(UUID membershipTierId) {
    return membershipTierCriteriaRepository.findByMembershipTierId(membershipTierId);
  }

  public Optional<MembershipTierCriteriaEntity> findByPriority(int priority) {
    return membershipTierCriteriaRepository.findByPriority(priority);
  }

  public List<MembershipTierCriteriaEntity> findAllOrderedByPriority() {
    return membershipTierCriteriaRepository.findAllOrderedByPriority();
  }
}
