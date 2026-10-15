package com.firstclub.membership.core.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.core.query.MembershipPerkFetchService;
import com.firstclub.membership.core.query.MembershipTierFetchService;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.DeliveryPerkApplication;
import com.firstclub.membership.models.request.admin.CreateMembershipTierRequest;
import com.firstclub.membership.models.request.admin.InitialMembershipPlanRequest;
import com.firstclub.membership.models.request.admin.NewMembershipPerkRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MembershipWriteValidatorTest {
  private final MembershipTierFetchService tierRepository = mock(MembershipTierFetchService.class);
  private final MembershipPerkFetchService perkRepository = mock(MembershipPerkFetchService.class);
  private final MembershipWriteValidator validator =
      new MembershipWriteValidator(tierRepository, perkRepository, new ObjectMapper());

  @Test
  void rejectsDuplicatePlanDurations() {
    when(tierRepository.findByDisplayNameIgnoreCase(any())).thenReturn(Optional.empty());
    assertThrows(
        ConflictException.class,
        () -> validator.validateCreateTier(request(List.of(plan(30), plan(30)), List.of())));
  }

  @Test
  void rejectsExistingTierNameIgnoringCase() {
    when(tierRepository.findByDisplayNameIgnoreCase("gold")).thenReturn(Optional.of(mock()));
    assertThrows(
        ConflictException.class,
        () -> validator.validateCreateTier(request(List.of(plan(30)), List.of())));
  }

  @Test
  void acceptsCaseInsensitivePerkDisplayKeyWhenNoActiveMatchExists() throws Exception {
    when(tierRepository.findByDisplayNameIgnoreCase(any())).thenReturn(Optional.empty());
    when(perkRepository.findActiveByAppliesToAndDisplayTextKey(any(), any()))
        .thenReturn(Optional.empty());
    var perk =
        new NewMembershipPerkRequest(
            List.of(new DisplayTextSegment("FREE Delivery", true)),
            PerkAppliesTo.DELIVERY,
            new DeliveryPerkApplication(new BigDecimal("100"), BigDecimal.ZERO, "INR"));
    assertDoesNotThrow(
        () -> validator.validateCreateTier(request(List.of(plan(30)), List.of(perk))));
  }

  private CreateMembershipTierRequest request(
      List<InitialMembershipPlanRequest> plans, List<NewMembershipPerkRequest> perks) {
    return new CreateMembershipTierRequest("gold", false, plans, perks, "test-admin");
  }

  private InitialMembershipPlanRequest plan(int duration) {
    return new InitialMembershipPlanRequest(duration, "Plan", BigDecimal.ONE, "INR", false);
  }
}
