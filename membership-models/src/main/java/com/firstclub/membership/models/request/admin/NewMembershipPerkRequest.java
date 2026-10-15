package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.PerkApplication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NewMembershipPerkRequest(
    @NotEmpty List<@Valid DisplayTextSegment> displayText,
    @NotNull PerkAppliesTo appliesTo,
    @NotNull @Valid PerkApplication perkApplication) {}
