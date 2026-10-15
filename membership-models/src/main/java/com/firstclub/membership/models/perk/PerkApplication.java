package com.firstclub.membership.models.perk;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.firstclub.membership.models.PerkAppliesTo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "applicationType")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DeliveryPerkApplication.class, name = "DELIVERY"),
  @JsonSubTypes.Type(value = CartDiscountPerkApplication.class, name = "CART_DISCOUNT"),
  @JsonSubTypes.Type(value = EarlyAccessSaleApplication.class, name = "EARLY_ACCESS_SALE"),
  @JsonSubTypes.Type(value = MembershipDealApplication.class, name = "MEMBERSHIP_DEAL"),
  @JsonSubTypes.Type(value = SupportPerkApplication.class, name = "SUPPORT")
})
public sealed interface PerkApplication
    permits DeliveryPerkApplication,
        CartDiscountPerkApplication,
        EarlyAccessSaleApplication,
        MembershipDealApplication,
        SupportPerkApplication {
  PerkAppliesTo appliesTo();
}
