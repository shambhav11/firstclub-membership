package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.RefundAllocationEntity;
import com.firstclub.membership.data.repository.RefundAllocationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RefundAllocationFetchService {
  private final RefundAllocationRepository refundAllocationRepository;

  public RefundAllocationFetchService(RefundAllocationRepository refundAllocationRepository) {
    this.refundAllocationRepository = refundAllocationRepository;
  }

  public Optional<RefundAllocationEntity> findByIdForUpdate(UUID allocationId) {
    return refundAllocationRepository.findByIdForUpdate(allocationId);
  }

  public Optional<RefundAllocationEntity> findById(UUID allocationId) {
    return refundAllocationRepository.findById(allocationId);
  }

  public List<RefundAllocationEntity> findByRefundId(UUID refundId) {
    return refundAllocationRepository.findByRefundId(refundId);
  }

  public Map<UUID, BigDecimal> sumReservedBySourcePaymentIds(Set<UUID> sourcePaymentIds) {
    return refundAllocationRepository.sumReservedBySourcePaymentIds(sourcePaymentIds);
  }
}
