package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.PaymentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
  Optional<PaymentEntity> findById(UUID id);

  Optional<PaymentEntity> findByIdForUpdate(UUID id);

  List<PaymentEntity> findByIds(java.util.Set<UUID> ids);

  Optional<PaymentEntity> findLatestBySubscriptionId(UUID subscriptionId);

  List<PaymentEntity> findLatestBySubscriptionIds(java.util.Set<UUID> subscriptionIds);

  Optional<PaymentEntity> findSuccessfulBySubscriptionId(UUID subscriptionId);

  List<PaymentEntity> findSuccessfulDirectPaymentsBySubscriptionId(UUID subscriptionId);

  List<PaymentEntity> findSuccessfulPaymentsBySubscriptionId(UUID subscriptionId);

  List<PaymentEntity> findBySubscriptionIdAndStatus(
      UUID subscriptionId, com.firstclub.membership.models.PaymentStatus status);

  List<PaymentEntity> findBySubscriptionId(UUID subscriptionId);

  List<PaymentEntity> findBySubscriptionIds(java.util.Set<UUID> subscriptionIds);

  PaymentEntity save(PaymentEntity payment);
}
