package com.firstclub.membership.models.perk;

import com.firstclub.membership.models.PerkAppliesTo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record DeliveryPerkApplication(
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100")
        BigDecimal discountPercent,
    @NotNull @DecimalMin("0") BigDecimal minOrderValue,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode)
    implements PerkApplication {
  @Override
  public PerkAppliesTo appliesTo() {
    return PerkAppliesTo.DELIVERY;
  }
}
