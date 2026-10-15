package com.firstclub.membership.core.command;

import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.data.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentWriteService {
  private final PaymentRepository paymentRepository;

  public PaymentWriteService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  public PaymentEntity save(PaymentEntity payment) {
    return paymentRepository.save(payment);
  }
}
