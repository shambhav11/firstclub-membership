package com.firstclub.membership.core.command;

import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.repository.MembershipSubscriptionRepository;
import org.springframework.stereotype.Service;

@Service
public class MembershipSubscriptionWriteService {
  private final MembershipSubscriptionRepository membershipSubscriptionRepository;

  public MembershipSubscriptionWriteService(
      MembershipSubscriptionRepository membershipSubscriptionRepository) {
    this.membershipSubscriptionRepository = membershipSubscriptionRepository;
  }

  public MembershipSubscriptionEntity save(MembershipSubscriptionEntity subscription) {
    return membershipSubscriptionRepository.save(subscription);
  }

  public void flush() {
    membershipSubscriptionRepository.flush();
  }
}
