package com.firstclub.membership.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.firstclub.membership")
@EntityScan(basePackages = "com.firstclub.membership.data.entity")
public class MembershipServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(MembershipServiceApplication.class, args);
  }
}
