package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.MembershipSubscriptionDao;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.repository.MembershipSubscriptionRepository;
import com.firstclub.membership.models.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipSubscriptionRepositoryImpl implements MembershipSubscriptionRepository {
  private final MembershipSubscriptionDao membershipSubscriptionDao;

  public MembershipSubscriptionRepositoryImpl(MembershipSubscriptionDao membershipSubscriptionDao) {
    this.membershipSubscriptionDao = membershipSubscriptionDao;
  }

  @Override
  public Optional<MembershipSubscriptionEntity> findByUserIdAndStatus(
      UUID userId, SubscriptionStatus status) {
    return membershipSubscriptionDao.findByUserIdAndStatus(userId, status);
  }

  @Override
  public Optional<MembershipSubscriptionEntity> findById(UUID id) {
    return membershipSubscriptionDao.findById(id);
  }

  @Override
  public MembershipSubscriptionEntity save(MembershipSubscriptionEntity subscription) {
    return membershipSubscriptionDao.save(subscription);
  }

  @Override
  public void flush() {
    membershipSubscriptionDao.flush();
  }

  @Override
  public Page<MembershipSubscriptionEntity> findByUserId(UUID userId, Pageable pageable) {
    return membershipSubscriptionDao.findByUserId(userId, pageable);
  }

  @Override
  public Optional<MembershipSubscriptionEntity> findLatestByUserId(UUID userId) {
    return membershipSubscriptionDao.findLatestByUserId(userId);
  }

  @Override
  public List<MembershipSubscriptionEntity> findActiveAutoRenewDueBy(
      Instant fromExclusive, Instant dueByInclusive) {
    return membershipSubscriptionDao.findActiveAutoRenewDueBy(fromExclusive, dueByInclusive);
  }

  @Override
  public List<MembershipSubscriptionEntity> findActiveExpiredBy(Instant expiryInclusive) {
    return membershipSubscriptionDao.findActiveExpiredBy(expiryInclusive);
  }

  @Override
  public List<MembershipSubscriptionEntity> findPendingPaymentWithExpiredCheckoutAttemptBy(
      Instant paymentExpiryInclusive) {
    return membershipSubscriptionDao.findPendingPaymentWithExpiredCheckoutAttemptBy(
        paymentExpiryInclusive);
  }
}
