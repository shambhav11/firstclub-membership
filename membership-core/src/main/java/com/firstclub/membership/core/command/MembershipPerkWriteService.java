package com.firstclub.membership.core.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.mappers.AdminMembershipResponseMapper;
import com.firstclub.membership.data.repository.MembershipPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.request.admin.CreateMembershipPerkRequest;
import com.firstclub.membership.models.request.admin.UpdateMembershipPerkRequest;
import com.firstclub.membership.models.response.admin.MembershipPerkResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipPerkWriteService {
  private static final Duration REACTIVATION_WINDOW = Duration.ofHours(24);
  private final MembershipPerkRepository membershipPerkRepository;
  private final MembershipTierPerkRepository membershipTierPerkRepository;
  private final MembershipTierRepository membershipTierRepository;
  private final AdminMembershipResponseMapper adminMembershipResponseMapper;
  private final ObjectMapper objectMapper;

  public MembershipPerkWriteService(
      MembershipPerkRepository membershipPerkRepository,
      MembershipTierPerkRepository membershipTierPerkRepository,
      MembershipTierRepository membershipTierRepository,
      AdminMembershipResponseMapper adminMembershipResponseMapper,
      ObjectMapper objectMapper) {
    this.membershipPerkRepository = membershipPerkRepository;
    this.membershipTierPerkRepository = membershipTierPerkRepository;
    this.membershipTierRepository = membershipTierRepository;
    this.adminMembershipResponseMapper = adminMembershipResponseMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public MembershipPerkResponse create(CreateMembershipPerkRequest request) {
    String key = displayTextKey(request);
    var existing =
        membershipPerkRepository.findActiveByAppliesToAndDisplayTextKey(request.appliesTo(), key);
    if (existing.isPresent()) return responseForExistingPerk(existing.get(), request);
    validateApplication(request);
    return createPerk(request, key);
  }

  private String displayTextKey(CreateMembershipPerkRequest request) {
    return request.displayText().stream()
        .map(segment -> segment.text().trim())
        .filter(text -> !text.isEmpty())
        .collect(java.util.stream.Collectors.joining(" "))
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private MembershipPerkResponse responseForExistingPerk(
      MembershipPerkEntity existing, CreateMembershipPerkRequest request) {
    if (!existing.getPerkApplication().equals(objectMapper.valueToTree(request.perkApplication()))) {
      throw new ConflictException("An active perk with the same display text has different configuration");
    }
    return adminMembershipResponseMapper.toPerk(existing, List.of());
  }

  private void validateApplication(CreateMembershipPerkRequest request) {
    if (request.appliesTo() != request.perkApplication().appliesTo()) {
      throw new ConflictException("Perk application does not match appliesTo");
    }
  }

  private MembershipPerkResponse createPerk(CreateMembershipPerkRequest request, String key) {
    MembershipPerkEntity perk =
        new MembershipPerkEntity(
                objectMapper.valueToTree(request.displayText()),
                key,
                request.appliesTo(),
                objectMapper.valueToTree(request.perkApplication()),
                PerkStatus.ACTIVE);
    perk.recordCreation(request.requestedBy());
    membershipPerkRepository.save(perk);
    membershipTierRepository.flush();
    return adminMembershipResponseMapper.toPerk(perk, List.of());
  }

  @Transactional
  public void updateStatus(UUID id, UpdateMembershipPerkRequest request) {
    var perk =
        membershipPerkRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Membership perk", id));
    if (membershipTierPerkRepository.countByPerkId(id) > 0) {
      throw new ConflictException("A perk attached to a tier cannot have its status changed");
    }
    if (request.status() == PerkStatus.ARCHIVED) {
      // Preserve updatedAt as the original archival time; an idempotent retry must not extend the
      // reactivation window.
      if (perk.getStatus() == PerkStatus.ARCHIVED) return;
      perk.archive();
    } else {
      reactivate(perk);
    }
    perk.recordUpdate(request.requestedBy());
    membershipTierRepository.flush();
  }

  private void reactivate(MembershipPerkEntity perk) {
    if (perk.getStatus() != PerkStatus.ARCHIVED) {
      throw new ConflictException("Only archived perks can be reactivated");
    }
    // Perk definitions are immutable. While a perk is archived, updatedAt is therefore the time
    // at which its status changed to ARCHIVED.
    Instant archivedAt = perk.getUpdatedAt();
    if (archivedAt == null || Instant.now().isAfter(archivedAt.plus(REACTIVATION_WINDOW))) {
      throw new ConflictException("A perk can be reactivated only within 24 hours of archival");
    }
    membershipPerkRepository
        .findActiveByAppliesToAndDisplayTextKey(perk.getAppliesTo(), perk.getDisplayTextKey())
        .ifPresent(
            existing -> {
              throw new ConflictException(
                  "An active perk with the same display text already exists in this application"
                      + " area");
            });
    perk.reactivate();
  }
}
