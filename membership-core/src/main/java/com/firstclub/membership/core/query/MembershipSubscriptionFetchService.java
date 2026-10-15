package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.repository.MembershipSubscriptionRepository;
import com.firstclub.membership.models.SubscriptionStatus;
import java.util.Optional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused subscription reads backed only by the subscription repository. */
@Service
@Transactional(readOnly = true)
public class MembershipSubscriptionFetchService {
  private final MembershipSubscriptionRepository membershipSubscriptionRepository;

  public MembershipSubscriptionFetchService(
      MembershipSubscriptionRepository membershipSubscriptionRepository) {
    this.membershipSubscriptionRepository = membershipSubscriptionRepository;
  }

  public Optional<MembershipSubscriptionEntity> findActiveByUserId(UUID userId) {
    return membershipSubscriptionRepository.findByUserIdAndStatus(
        userId, SubscriptionStatus.ACTIVE);
  }

  public Optional<MembershipSubscriptionEntity> findByUserIdAndStatus(
      UUID userId, SubscriptionStatus status) {
    return membershipSubscriptionRepository.findByUserIdAndStatus(userId, status);
  }

  public Optional<MembershipSubscriptionEntity> findById(UUID subscriptionId) {
    return membershipSubscriptionRepository.findById(subscriptionId);
  }

  public Optional<MembershipSubscriptionEntity> findLatestByUserId(UUID userId) {
    return membershipSubscriptionRepository.findLatestByUserId(userId);
  }

  public List<MembershipSubscriptionEntity> findActiveAutoRenewDueBy(Instant now, Instant dueBy) {
    return membershipSubscriptionRepository.findActiveAutoRenewDueBy(now, dueBy);
  }

  public List<MembershipSubscriptionEntity> findActiveExpiredBy(Instant now) {
    return membershipSubscriptionRepository.findActiveExpiredBy(now);
  }

  public List<MembershipSubscriptionEntity> findPendingPaymentWithExpiredCheckoutAttemptBy(Instant now) {
    return membershipSubscriptionRepository.findPendingPaymentWithExpiredCheckoutAttemptBy(now);
  }

  public Page<MembershipSubscriptionEntity> getHistory(UUID userId, Pageable pageable) {
    return membershipSubscriptionRepository.findByUserId(userId, pageable);
  }
}
