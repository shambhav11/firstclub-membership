package com.firstclub.membership.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.firstclub.membership.models.PerkAppliesTo;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in smoke test against a locally running Membership service.
 *
 * <p>Run with {@code mvn -pl membership-client test -Dmembership.client.live.enabled=true} after
 * starting the service on localhost:8080.
 */
@EnabledIfSystemProperty(named = "membership.client.live.enabled", matches = "true")
class UserMembershipClientLiveIntegrationTest {
  private static final UserMembershipClient client =
      new UserMembershipClient("http://localhost:8080");

  @Test
  void fetchesTheCatalogAndFiltersUserPerksByMultipleApplications() throws Exception {
    assertNotNull(client.getMembershipOptions().get(10, TimeUnit.SECONDS));
    assertNotNull(
        client
            .getActiveMembershipPerks(
                UUID.randomUUID(), List.of(PerkAppliesTo.DELIVERY, PerkAppliesTo.CART))
            .get(10, TimeUnit.SECONDS));
  }
}
