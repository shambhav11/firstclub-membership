package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.data.repository.PaymentRepository;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused payment reads backed only by the payment repository. */
@Service
@Transactional(readOnly = true)
public class PaymentFetchService {
  private final PaymentRepository paymentRepository;

  public PaymentFetchService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  public Optional<PaymentEntity> findLatestBySubscriptionId(UUID subscriptionId) {
    return paymentRepository.findLatestBySubscriptionId(subscriptionId);
  }

  public Map<UUID, PaymentEntity> findLatestBySubscriptionIds(Set<UUID> subscriptionIds) {
    Map<UUID, PaymentEntity> latest = new java.util.HashMap<>();
    for (PaymentEntity payment : paymentRepository.findLatestBySubscriptionIds(subscriptionIds)) {
      latest.putIfAbsent(payment.getSubscriptionId(), payment);
    }
    return latest;
  }

  public Optional<PaymentEntity> findById(UUID paymentId) {
    return paymentRepository.findById(paymentId);
  }

  public Optional<PaymentEntity> findByIdForUpdate(UUID paymentId) {
    return paymentRepository.findByIdForUpdate(paymentId);
  }

  public List<PaymentEntity> findByIds(Set<UUID> paymentIds) {
    return paymentRepository.findByIds(paymentIds);
  }

  public List<PaymentEntity> findSuccessfulBySubscriptionId(UUID subscriptionId) {
    return paymentRepository.findSuccessfulPaymentsBySubscriptionId(subscriptionId);
  }

  public List<PaymentEntity> findBySubscriptionIds(Set<UUID> subscriptionIds) {
    return paymentRepository.findBySubscriptionIds(subscriptionIds);
  }
}
