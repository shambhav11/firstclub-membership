package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.RefundDao;
import com.firstclub.membership.data.entity.RefundEntity;
import com.firstclub.membership.data.repository.RefundRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefundRepositoryImpl implements RefundRepository {
  private final RefundDao refundDao;

  public RefundRepositoryImpl(RefundDao refundDao) {
    this.refundDao = refundDao;
  }

  public Optional<RefundEntity> findBySubscriptionId(UUID id) {
    return refundDao.findBySubscriptionId(id);
  }

  @Override
  public java.util.List<RefundEntity> findBySubscriptionIds(java.util.Set<UUID> subscriptionIds) {
    return refundDao.findBySubscriptionIds(subscriptionIds);
  }

  public Optional<RefundEntity> findById(UUID id) {
    return refundDao.findById(id);
  }

  @Override
  public Optional<RefundEntity> findByIdForUpdate(UUID id) {
    return refundDao.findByIdForUpdate(id);
  }

  public RefundEntity save(RefundEntity refund) {
    return refundDao.save(refund);
  }
}
