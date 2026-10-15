package com.firstclub.membership.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Standalone scheduler process, deployed as a single replica independently of the API. */
@SpringBootApplication(scanBasePackages = "com.firstclub.membership")
@EntityScan(basePackages = "com.firstclub.membership.data.entity")
@EnableScheduling
public class MembershipJobsApplication {
  public static void main(String[] args) {
    SpringApplication.run(MembershipJobsApplication.class, args);
  }
}
