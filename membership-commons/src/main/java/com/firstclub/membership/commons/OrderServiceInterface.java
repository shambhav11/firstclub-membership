package com.firstclub.membership.commons;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Boundary owned by the external user service. */
@Component
public class OrderServiceInterface {

  private final Map<UUID, OrderFacts> factsByUser = new ConcurrentHashMap<>();

  // TODO: Add service client

  public OrderServiceInterface() {}

  public int getOrderCountForUser(UUID userId) {
    return factsByUser.getOrDefault(userId, OrderFacts.EMPTY).orderCount();
  }

  public BigDecimal getOrderValueForUser(UUID userId) {
    return factsByUser.getOrDefault(userId, OrderFacts.EMPTY).orderValue();
  }

  /**
   * Boundary for a real order-service client to aggregate orders in the half-open timestamp range
   * {@code [fromInclusive, toExclusive)}.
   */
  public int getOrderCountForUserBetweenTimestamps(
      UUID userId, Instant fromInclusive, Instant toExclusive) {
    return getOrderCountForUser(userId);
  }

  /**
   * Boundary for a real order-service client to aggregate order value in the half-open timestamp
   * range {@code [fromInclusive, toExclusive)}.
   */
  public BigDecimal getOrderValueForUserBetweenTimestamps(
      UUID userId, Instant fromInclusive, Instant toExclusive) {
    return getOrderValueForUser(userId);
  }

  /** Replaced by the eventual order-service client; exposed only for the assignment test API. */
  public void setFactsForTesting(UUID userId, int orderCount, BigDecimal orderValue) {
    factsByUser.put(userId, new OrderFacts(orderCount, orderValue));
  }

  private record OrderFacts(int orderCount, BigDecimal orderValue) {
    private static final OrderFacts EMPTY = new OrderFacts(0, BigDecimal.ZERO);
  }
}
