package com.firstclub.membership.models.perk;

import com.firstclub.membership.models.PerkAppliesTo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SupportPerkApplication(
    @NotNull CallbackWithinUnit callbackWithinUnit, @Min(1) int callbackWithinValue)
    implements PerkApplication {
  @Override
  public PerkAppliesTo appliesTo() {
    return PerkAppliesTo.SUPPORT;
  }
}
