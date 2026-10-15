package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.PaymentDao;
import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.data.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
  private final PaymentDao paymentDao;

  public PaymentRepositoryImpl(PaymentDao paymentDao) {
    this.paymentDao = paymentDao;
  }

  @Override
  public Optional<PaymentEntity> findById(UUID id) {
    return paymentDao.findById(id);
  }

  @Override
  public Optional<PaymentEntity> findByIdForUpdate(UUID id) {
    return paymentDao.findByIdForUpdate(id);
  }

  @Override
  public List<PaymentEntity> findByIds(java.util.Set<UUID> ids) {
    return paymentDao.findByIds(ids);
  }

  @Override
  public Optional<PaymentEntity> findLatestBySubscriptionId(UUID subscriptionId) {
    return paymentDao.findLatestBySubscriptionId(subscriptionId);
  }

  @Override
  public List<PaymentEntity> findLatestBySubscriptionIds(java.util.Set<UUID> subscriptionIds) {
    return paymentDao.findLatestBySubscriptionIds(subscriptionIds);
  }

  @Override
  public Optional<PaymentEntity> findSuccessfulBySubscriptionId(UUID subscriptionId) {
    return paymentDao.findSuccessfulBySubscriptionId(subscriptionId);
  }

  @Override
  public List<PaymentEntity> findSuccessfulDirectPaymentsBySubscriptionId(UUID subscriptionId) {
    return paymentDao.findSuccessfulDirectPaymentsBySubscriptionId(subscriptionId);
  }

  @Override
  public List<PaymentEntity> findSuccessfulPaymentsBySubscriptionId(UUID subscriptionId) {
    return paymentDao.findSuccessfulPaymentsBySubscriptionId(subscriptionId);
  }

  @Override
  public List<PaymentEntity> findBySubscriptionIdAndStatus(
      UUID subscriptionId, com.firstclub.membership.models.PaymentStatus status) {
    return paymentDao.findBySubscriptionIdAndStatus(subscriptionId, status);
  }

  @Override
  public List<PaymentEntity> findBySubscriptionId(UUID subscriptionId) {
    return paymentDao.findBySubscriptionId(subscriptionId);
  }

  @Override
  public List<PaymentEntity> findBySubscriptionIds(java.util.Set<UUID> subscriptionIds) {
    return paymentDao.findBySubscriptionIds(subscriptionIds);
  }

  @Override
  public PaymentEntity save(PaymentEntity payment) {
    return paymentDao.save(payment);
  }
}
