package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.RefundAllocationEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RefundAllocationRepository {
  RefundAllocationEntity save(RefundAllocationEntity allocation);

  java.util.Optional<RefundAllocationEntity> findById(UUID id);

  java.util.Optional<RefundAllocationEntity> findByIdForUpdate(UUID id);

  List<RefundAllocationEntity> findByRefundId(UUID refundId);

  BigDecimal sumReservedBySourcePaymentId(UUID sourcePaymentId);

  java.util.Map<UUID, BigDecimal> sumReservedBySourcePaymentIds(
      java.util.Set<UUID> sourcePaymentIds);
}
