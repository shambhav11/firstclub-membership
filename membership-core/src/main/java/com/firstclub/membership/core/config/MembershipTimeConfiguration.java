package com.firstclub.membership.core.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MembershipTimeConfiguration {
  @Bean
  Clock membershipClock() {
    return Clock.system(ZoneId.of("Asia/Kolkata"));
  }
}
