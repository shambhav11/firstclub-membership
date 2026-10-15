package com.firstclub.membership.core.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.mappers.AdminMembershipResponseMapper;
import com.firstclub.membership.data.repository.MembershipPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.request.admin.UpdateMembershipPerkRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipPerkWriteServiceTest {
  private final MembershipPerkRepository perkRepository = mock(MembershipPerkRepository.class);
  private final MembershipTierPerkRepository tierPerkRepository =
      mock(MembershipTierPerkRepository.class);
  private final MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
  private final MembershipPerkWriteService service =
      new MembershipPerkWriteService(
          perkRepository,
          tierPerkRepository,
          tierRepository,
          mock(AdminMembershipResponseMapper.class),
          new ObjectMapper());

  @Test
  void reactivatesAnUnassignedPerkWithinTwentyFourHoursOfArchival() {
    UUID perkId = UUID.randomUUID();
    MembershipPerkEntity perk = archivedPerk(Instant.now().minusSeconds(23 * 60 * 60));
    when(perkRepository.findById(perkId)).thenReturn(Optional.of(perk));
    when(tierPerkRepository.countByPerkId(perkId)).thenReturn(0L);
    when(perkRepository.findActiveByAppliesToAndDisplayTextKey(PerkAppliesTo.DELIVERY, "delivery"))
        .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> service.updateStatus(perkId, activateRequest()));

    verify(perk).reactivate();
    verify(tierRepository).flush();
  }

  @Test
  void rejectsReactivationMoreThanTwentyFourHoursAfterArchival() {
    UUID perkId = UUID.randomUUID();
    MembershipPerkEntity perk = archivedPerk(Instant.now().minusSeconds(24 * 60 * 60 + 1));
    when(perkRepository.findById(perkId)).thenReturn(Optional.of(perk));
    when(tierPerkRepository.countByPerkId(perkId)).thenReturn(0L);

    assertThrows(ConflictException.class, () -> service.updateStatus(perkId, activateRequest()));

    verify(perk, never()).reactivate();
    verify(tierRepository, never()).flush();
  }

  @Test
  void repeatedArchiveDoesNotRefreshTheReactivationWindow() {
    UUID perkId = UUID.randomUUID();
    MembershipPerkEntity perk = archivedPerk(Instant.now().minusSeconds(23 * 60 * 60));
    when(perkRepository.findById(perkId)).thenReturn(Optional.of(perk));
    when(tierPerkRepository.countByPerkId(perkId)).thenReturn(0L);

    assertDoesNotThrow(
        () ->
            service.updateStatus(
                perkId, new UpdateMembershipPerkRequest(PerkStatus.ARCHIVED, "test-admin")));

    verify(perk, never()).archive();
    verify(tierRepository, never()).flush();
  }

  private MembershipPerkEntity archivedPerk(Instant archivedAt) {
    MembershipPerkEntity perk = mock(MembershipPerkEntity.class);
    when(perk.getStatus()).thenReturn(PerkStatus.ARCHIVED);
    when(perk.getUpdatedAt()).thenReturn(archivedAt);
    when(perk.getAppliesTo()).thenReturn(PerkAppliesTo.DELIVERY);
    when(perk.getDisplayTextKey()).thenReturn("delivery");
    return perk;
  }

  private UpdateMembershipPerkRequest activateRequest() {
    return new UpdateMembershipPerkRequest(PerkStatus.ACTIVE, "test-admin");
  }
}
