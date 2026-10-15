package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.RefundAllocationDao;
import com.firstclub.membership.data.entity.RefundAllocationEntity;
import com.firstclub.membership.data.repository.RefundAllocationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefundAllocationRepositoryImpl implements RefundAllocationRepository {
  private final RefundAllocationDao dao;

  public RefundAllocationRepositoryImpl(RefundAllocationDao dao) {
    this.dao = dao;
  }

  @Override
  public RefundAllocationEntity save(RefundAllocationEntity allocation) {
    return dao.save(allocation);
  }

  @Override
  public java.util.Optional<RefundAllocationEntity> findById(UUID id) {
    return dao.findById(id);
  }

  @Override
  public java.util.Optional<RefundAllocationEntity> findByIdForUpdate(UUID id) {
    return dao.findByIdForUpdate(id);
  }

  @Override
  public List<RefundAllocationEntity> findByRefundId(UUID refundId) {
    return dao.findByRefundId(refundId);
  }

  @Override
  public BigDecimal sumReservedBySourcePaymentId(UUID sourcePaymentId) {
    return dao.sumReservedBySourcePaymentId(sourcePaymentId);
  }

  @Override
  public java.util.Map<UUID, BigDecimal> sumReservedBySourcePaymentIds(
      java.util.Set<UUID> sourcePaymentIds) {
    return dao.sumReservedBySourcePaymentIds(sourcePaymentIds);
  }
}
