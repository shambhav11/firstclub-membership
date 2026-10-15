package com.firstclub.membership.data.entity;

import com.firstclub.membership.models.PaymentStatus;
import com.firstclub.membership.models.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "payments")
public class PaymentEntity extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "subscription_id", nullable = false)
  private UUID subscriptionId;

  @Column(name = "transaction_id")
  private String transactionId;

  @Column(name = "bank_reference_id")
  private String bankReferenceId;

  /**
   * The gateway which owns the payment method reference, for example Razorpay or Stripe. It is
   * deliberately nullable until a payment gateway is integrated.
   */
  @Column(name = "payment_gateway", length = 50)
  private String paymentGateway;

  /**
   * A token or mandate reference issued by {@link #paymentGateway}; never raw card or bank data.
   */
  @Column(name = "payment_method_reference", length = 255)
  private String paymentMethodReference;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type", nullable = false)
  private PaymentType paymentType;

  @Column(name = "source_subscription_id")
  private UUID sourceSubscriptionId;

  @Column(name = "source_payment_id")
  private UUID sourcePaymentId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "payment_expires_at")
  private Instant paymentExpiresAt;

  protected PaymentEntity() {}

  public PaymentEntity(
      UUID subscriptionId,
      String transactionId,
      BigDecimal amount,
      String currencyCode,
      Instant paymentExpiresAt) {
    this.subscriptionId = subscriptionId;
    this.transactionId = transactionId;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.paymentExpiresAt = paymentExpiresAt;
    this.status = PaymentStatus.PENDING;
    this.paymentType = PaymentType.DIRECT_PAYMENT;
  }

  public static PaymentEntity membershipCredit(
      UUID subscriptionId,
      BigDecimal amount,
      String currencyCode,
      UUID sourceSubscriptionId,
      UUID sourcePaymentId) {
    PaymentEntity payment = new PaymentEntity();
    payment.subscriptionId = subscriptionId;
    payment.amount = amount;
    payment.currencyCode = currencyCode;
    payment.status = PaymentStatus.SUCCESS;
    payment.paymentType = PaymentType.MEMBERSHIP_CREDIT;
    payment.sourceSubscriptionId = sourceSubscriptionId;
    payment.sourcePaymentId = sourcePaymentId;
    return payment;
  }

  public UUID getId() {
    return id;
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public PaymentType getPaymentType() {
    return paymentType;
  }

  public UUID getSourceSubscriptionId() {
    return sourceSubscriptionId;
  }

  public UUID getSourcePaymentId() {
    return sourcePaymentId;
  }

  public Instant getPaymentExpiresAt() {
    return paymentExpiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void complete(String bankReferenceId) {
    this.status = PaymentStatus.SUCCESS;
    this.bankReferenceId = bankReferenceId;
  }

  public void fail() {
    this.status = PaymentStatus.FAILED;
  }

  public void expire() {
    this.status = PaymentStatus.EXPIRED;
  }

  public void voidPayment() {
    this.status = PaymentStatus.VOIDED;
  }
}
