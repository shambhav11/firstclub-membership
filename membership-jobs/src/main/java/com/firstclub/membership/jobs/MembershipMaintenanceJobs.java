package com.firstclub.membership.jobs;

import com.firstclub.membership.core.command.MembershipMaintenanceManager;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduler shell; maintenance services are added as each lifecycle query is completed. */
@Component
public class MembershipMaintenanceJobs {
  private final MembershipMaintenanceManager membershipMaintenanceManager;
  private final Duration autoRenewLeadTime;
  private final Duration paymentExpiry;

  public MembershipMaintenanceJobs(
      MembershipMaintenanceManager membershipMaintenanceManager,
      @org.springframework.beans.factory.annotation.Value(
              "${membership.jobs.auto-renew-lead-time:PT24H}")
          Duration autoRenewLeadTime,
      @org.springframework.beans.factory.annotation.Value("${membership.jobs.payment-expiry:PT15M}")
          Duration paymentExpiry) {
    this.membershipMaintenanceManager = membershipMaintenanceManager;
    this.autoRenewLeadTime = autoRenewLeadTime;
    this.paymentExpiry = paymentExpiry;
  }

  @Scheduled(fixedDelayString = "${membership.jobs.maintenance-delay-ms:60000}")
  public void runMaintenance() {
    membershipMaintenanceManager.runMaintenance(
        OffsetDateTime.now(ZoneId.of("Asia/Kolkata")), autoRenewLeadTime, paymentExpiry);
  }
}
