package com.firstclub.membership.models.perk;

import com.firstclub.membership.models.PerkAppliesTo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record MembershipDealApplication(@NotEmpty Set<@NotBlank String> applicableDealTiers)
    implements PerkApplication {
  @Override
  public PerkAppliesTo appliesTo() {
    return PerkAppliesTo.MEMBERSHIP_DEAL;
  }
}
