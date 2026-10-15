package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.mappers.AdminMembershipResponseMapper;
import com.firstclub.membership.data.repository.MembershipPlanRepository;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.request.admin.CreateMembershipPlanRequest;
import com.firstclub.membership.models.request.admin.UpdateMembershipPlanRequest;
import com.firstclub.membership.models.response.admin.MembershipPlanResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipPlanWriteService {
  private final MembershipPlanRepository membershipPlanRepository;
  private final MembershipTierRepository membershipTierRepository;
  private final AdminMembershipResponseMapper adminMembershipResponseMapper;

  public MembershipPlanWriteService(
      MembershipPlanRepository membershipPlanRepository,
      MembershipTierRepository membershipTierRepository,
      AdminMembershipResponseMapper adminMembershipResponseMapper) {
    this.membershipPlanRepository = membershipPlanRepository;
    this.membershipTierRepository = membershipTierRepository;
    this.adminMembershipResponseMapper = adminMembershipResponseMapper;
  }

  @Transactional
  public MembershipPlanResponse create(CreateMembershipPlanRequest request) {
    if (membershipTierRepository.findById(request.membershipTierId()).isEmpty()) {
      throw new ResourceNotFoundException("Membership tier", request.membershipTierId());
    }
    if (membershipPlanRepository.findAllByMembershipTierId(request.membershipTierId()).stream()
        .anyMatch(plan -> plan.getDurationDays() == request.durationDays())) {
      throw new ConflictException("A plan with this duration already exists for the tier");
    }
    var plan =
        new MembershipPlanEntity(
                request.membershipTierId(),
                request.durationDays(),
                request.displayText().trim(),
                request.amount(),
                request.currencyCode().trim().toUpperCase(),
                PlanStatus.ACTIVE,
                request.enrollmentClosed());
    plan.recordCreation(request.requestedBy());
    membershipPlanRepository.save(plan);
    membershipTierRepository.flush();
    return adminMembershipResponseMapper.toPlan(plan);
  }

  @Transactional
  public MembershipPlanResponse update(UUID id, UpdateMembershipPlanRequest request) {
    var plan =
        membershipPlanRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Membership plan", id));
    plan.update(
        request.displayText().trim(),
        request.amount(),
        request.currencyCode().trim().toUpperCase(),
        request.status(),
        request.enrollmentClosed());
    plan.recordUpdate(request.requestedBy());
    membershipTierRepository.flush();
    return adminMembershipResponseMapper.toPlan(plan);
  }
}
