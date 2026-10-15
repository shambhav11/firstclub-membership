package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.RefundAllocationFetchService;
import com.firstclub.membership.core.query.RefundFetchService;
import com.firstclub.membership.models.RefundStatus;
import com.firstclub.membership.models.response.user.RefundSummaryResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Assignment-only stand-in for refund gateway webhooks. */
@Service
public class TestingRefundSettlementService {
  private final RefundAllocationFetchService refundAllocationFetchService;
  private final RefundFetchService refundFetchService;
  private final MembershipSubscriptionFetchService subscriptionFetchService;
  private final UserCheckoutTransactionExecutor transactionExecutor;

  public TestingRefundSettlementService(
      RefundAllocationFetchService refundAllocationFetchService,
      RefundFetchService refundFetchService,
      MembershipSubscriptionFetchService subscriptionFetchService,
      UserCheckoutTransactionExecutor transactionExecutor) {
    this.refundAllocationFetchService = refundAllocationFetchService;
    this.refundFetchService = refundFetchService;
    this.subscriptionFetchService = subscriptionFetchService;
    this.transactionExecutor = transactionExecutor;
  }

  public RefundSummaryResponse complete(UUID allocationId, String reference, String actor) {
    return transactionExecutor.execute(
        allocationUserId(allocationId), () -> update(allocationId, true, reference, actor));
  }

  public RefundSummaryResponse fail(UUID allocationId, String reason, String actor) {
    return transactionExecutor.execute(
        allocationUserId(allocationId), () -> update(allocationId, false, reason, actor));
  }

  private RefundSummaryResponse update(UUID allocationId, boolean complete, String value, String actor) {
    var allocation =
        refundAllocationFetchService
            .findByIdForUpdate(allocationId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund allocation", allocationId));
    if (isRepeatedCallback(allocation, complete)) return responseFor(allocation.getRefundId());
    requirePendingAllocation(allocation);
    updateAllocation(allocation, complete, value, actor);
    var refund =
        refundFetchService
            .findByIdForUpdate(allocation.getRefundId())
            .orElseThrow(() -> new ResourceNotFoundException("Refund", allocation.getRefundId()));
    updateRefundStatus(refund, complete, actor);
    return toResponse(refund);
  }

  private boolean isRepeatedCallback(
      com.firstclub.membership.data.entity.RefundAllocationEntity allocation, boolean complete) {
    return complete && allocation.getStatus() == RefundStatus.COMPLETED
        || !complete && allocation.getStatus() == RefundStatus.FAILED;
  }

  private void requirePendingAllocation(
      com.firstclub.membership.data.entity.RefundAllocationEntity allocation) {
    if (allocation.getStatus() != RefundStatus.PENDING) {
      throw new ConflictException("Refund allocation is no longer pending");
    }
  }

  private void updateAllocation(
      com.firstclub.membership.data.entity.RefundAllocationEntity allocation,
      boolean complete,
      String value,
      String actor) {
    if (complete) allocation.complete(value);
    else allocation.fail(value);
    allocation.recordUpdate(actor);
  }

  private void updateRefundStatus(
      com.firstclub.membership.data.entity.RefundEntity refund, boolean complete, String actor) {
    if (!complete) {
      refund.updateStatus(RefundStatus.FAILED);
      refund.recordUpdate(actor);
      return;
    }
    if (refundAllocationFetchService.findByRefundId(refund.getId()).stream()
        .allMatch(item -> item.getStatus() == RefundStatus.COMPLETED)) {
      refund.updateStatus(RefundStatus.COMPLETED);
      refund.recordUpdate(actor);
    }
  }

  private UUID allocationUserId(UUID allocationId) {
    var allocation =
        refundAllocationFetchService
            .findById(allocationId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund allocation", allocationId));
    var refund =
        refundFetchService
            .findById(allocation.getRefundId())
            .orElseThrow(() -> new ResourceNotFoundException("Refund", allocation.getRefundId()));
    return subscriptionFetchService
        .findById(refund.getSubscriptionId())
        .orElseThrow(
            () -> new ResourceNotFoundException("Membership subscription", refund.getSubscriptionId()))
        .getUserId();
  }

  private RefundSummaryResponse responseFor(UUID refundId) {
    var refund =
        refundFetchService
            .findByIdForUpdate(refundId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
    return toResponse(refund);
  }

  private RefundSummaryResponse toResponse(com.firstclub.membership.data.entity.RefundEntity refund) {
    return new RefundSummaryResponse(
        refund.getId(),
        refund.getAmount(),
        refund.getCurrencyCode(),
        refund.getStatus(),
        refund.getCreatedAt() == null
            ? null
            : refund.getCreatedAt().atZone(java.time.ZoneId.of("Asia/Kolkata")).toOffsetDateTime());
  }
}
