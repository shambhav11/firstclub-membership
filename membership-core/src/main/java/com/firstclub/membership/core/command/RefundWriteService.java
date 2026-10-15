package com.firstclub.membership.core.command;

import com.firstclub.membership.data.entity.RefundEntity;
import com.firstclub.membership.data.repository.RefundRepository;
import org.springframework.stereotype.Service;

@Service
public class RefundWriteService {
  private final RefundRepository refundRepository;

  public RefundWriteService(RefundRepository refundRepository) {
    this.refundRepository = refundRepository;
  }

  public RefundEntity save(RefundEntity refund) {
    return refundRepository.save(refund);
  }
}
