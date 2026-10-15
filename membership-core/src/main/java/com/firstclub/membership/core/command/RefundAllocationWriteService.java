package com.firstclub.membership.core.command;

import com.firstclub.membership.data.entity.RefundAllocationEntity;
import com.firstclub.membership.data.repository.RefundAllocationRepository;
import org.springframework.stereotype.Service;

@Service
public class RefundAllocationWriteService {
  private final RefundAllocationRepository refundAllocationRepository;

  public RefundAllocationWriteService(RefundAllocationRepository refundAllocationRepository) {
    this.refundAllocationRepository = refundAllocationRepository;
  }

  public RefundAllocationEntity save(RefundAllocationEntity allocation) {
    return refundAllocationRepository.save(allocation);
  }
}
