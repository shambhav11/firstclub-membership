package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.models.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembershipSubscriptionRepository {
  Optional<MembershipSubscriptionEntity> findByUserIdAndStatus(
      UUID userId, SubscriptionStatus status);

  Optional<MembershipSubscriptionEntity> findById(UUID id);

  MembershipSubscriptionEntity save(MembershipSubscriptionEntity subscription);

  void flush();

  Page<MembershipSubscriptionEntity> findByUserId(UUID userId, Pageable pageable);

  Optional<MembershipSubscriptionEntity> findLatestByUserId(UUID userId);

  List<MembershipSubscriptionEntity> findActiveAutoRenewDueBy(
      Instant fromExclusive, Instant dueByInclusive);

  List<MembershipSubscriptionEntity> findActiveExpiredBy(Instant expiryInclusive);

  List<MembershipSubscriptionEntity> findPendingPaymentWithExpiredCheckoutAttemptBy(
      Instant paymentExpiryInclusive);
}
