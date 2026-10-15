package com.firstclub.membership.models.perk;

import com.firstclub.membership.models.PerkAppliesTo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record CartDiscountPerkApplication(
    @Min(1) @Max(10_000) int discountPercentBasisPoints,
    @NotEmpty Set<@NotBlank String> eligibleVerticals)
    implements PerkApplication {
  @Override
  public PerkAppliesTo appliesTo() {
    return PerkAppliesTo.CART;
  }
}
