package com.firstclub.membership.core.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.core.query.MembershipPerkFetchService;
import com.firstclub.membership.core.query.MembershipTierFetchService;
import com.firstclub.membership.models.request.admin.CreateMembershipTierRequest;
import com.firstclub.membership.models.request.admin.NewMembershipPerkRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MembershipWriteValidator {
  private final MembershipTierFetchService tierFetchService;
  private final MembershipPerkFetchService perkFetchService;
  private final ObjectMapper objectMapper;

  public MembershipWriteValidator(
      MembershipTierFetchService tierFetchService,
      MembershipPerkFetchService perkFetchService,
      ObjectMapper objectMapper) {
    this.tierFetchService = tierFetchService;
    this.perkFetchService = perkFetchService;
    this.objectMapper = objectMapper;
  }

  public void validateCreateTier(CreateMembershipTierRequest request) {
    validateTierName(request);
    validatePlanDurations(request);
    validatePerks(request);
  }

  private void validateTierName(CreateMembershipTierRequest request) {
    if (tierFetchService.findByDisplayNameIgnoreCase(request.displayName()).isPresent()) {
      throw new ConflictException("A membership tier with this display name already exists");
    }
  }

  private void validatePlanDurations(CreateMembershipTierRequest request) {
    Set<Integer> durations = new HashSet<>();
    request.initialPlans().forEach(plan -> validatePlanDuration(durations, plan.durationDays()));
  }

  private void validatePlanDuration(Set<Integer> durations, int durationDays) {
    if (!durations.add(durationDays)) {
      throw new ConflictException("Duplicate plan duration in request");
    }
  }

  private void validatePerks(CreateMembershipTierRequest request) {
    Map<String, com.firstclub.membership.models.perk.PerkApplication> configurations =
        new HashMap<>();
    for (NewMembershipPerkRequest perk : request.perks()) {
      validatePerk(perk, configurations);
    }
  }

  private void validatePerk(
      NewMembershipPerkRequest perk,
      Map<String, com.firstclub.membership.models.perk.PerkApplication> configurations) {
    if (perk.appliesTo() != perk.perkApplication().appliesTo()) {
      throw new ConflictException("Perk application does not match appliesTo");
    }
    String key = displayTextKey(perk);
    validateDuplicatePerk(perk, configurations, key);
    validateExistingPerk(perk, key);
  }

  private void validateDuplicatePerk(
      NewMembershipPerkRequest perk,
      Map<String, com.firstclub.membership.models.perk.PerkApplication> configurations,
      String key) {
    String requestKey = perk.appliesTo() + ":" + key;
    var previous = configurations.putIfAbsent(requestKey, perk.perkApplication());
    if (previous != null && !previous.equals(perk.perkApplication())) {
      throw new ConflictException("Conflicting perk definitions in request");
    }
  }

  private void validateExistingPerk(NewMembershipPerkRequest perk, String key) {
    perkFetchService
        .findActiveByAppliesToAndDisplayTextKey(perk.appliesTo(), key)
        .ifPresent(
            existing -> {
              if (!existing.getPerkApplication().equals(objectMapper.valueToTree(perk.perkApplication()))) {
                throw new ConflictException(
                    "An active perk with the same display text has different configuration");
              }
            });
  }

  public String displayTextKey(NewMembershipPerkRequest perk) {
    return perk.displayText().stream()
        .map(segment -> segment.text().trim())
        .filter(text -> !text.isEmpty())
        .collect(java.util.stream.Collectors.joining(" "))
        .trim()
        .replaceAll("\\s+", " ")
        .toLowerCase(java.util.Locale.ROOT);
  }
}
