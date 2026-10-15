package com.firstclub.membership.models.perk;

import com.firstclub.membership.models.PerkAppliesTo;
import jakarta.validation.constraints.Min;

public record EarlyAccessSaleApplication(@Min(0) int offsetValueInHours)
    implements PerkApplication {
  @Override
  public PerkAppliesTo appliesTo() {
    return PerkAppliesTo.SALE;
  }
}
