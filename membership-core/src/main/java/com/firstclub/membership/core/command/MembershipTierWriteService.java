package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.mappers.AdminMembershipResponseMapper;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.request.admin.UpdateMembershipTierRequest;
import com.firstclub.membership.models.response.admin.MembershipTierSummaryResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipTierWriteService {
  private final MembershipTierRepository membershipTierRepository;
  private final AdminMembershipResponseMapper adminMembershipResponseMapper;

  public MembershipTierWriteService(
      MembershipTierRepository membershipTierRepository,
      AdminMembershipResponseMapper adminMembershipResponseMapper) {
    this.membershipTierRepository = membershipTierRepository;
    this.adminMembershipResponseMapper = adminMembershipResponseMapper;
  }

  @Transactional
  public MembershipTierSummaryResponse update(UUID id, UpdateMembershipTierRequest request) {
    var tier =
        membershipTierRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Membership tier", id));
    membershipTierRepository
        .findByDisplayNameIgnoreCase(request.displayName())
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(
            existing -> {
              throw new ConflictException(
                  "A membership tier with this display name already exists");
            });
    tier.update(request.displayName().trim(), request.status(), request.enrollmentClosed());
    tier.recordUpdate(request.requestedBy());
    membershipTierRepository.flush();
    return adminMembershipResponseMapper.toTierSummary(tier);
  }
}
