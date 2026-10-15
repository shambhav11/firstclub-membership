package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.RedisLock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Holds the per-user lease until the transaction has committed or rolled back. */
@Component
public class UserCheckoutTransactionExecutor {
  private final RedisLock userCheckoutLock;
  private final TransactionTemplate transactionTemplate;

  public UserCheckoutTransactionExecutor(
      @Qualifier("userCheckoutLock") RedisLock userCheckoutLock,
      TransactionTemplate transactionTemplate) {
    this.userCheckoutLock = userCheckoutLock;
    this.transactionTemplate = transactionTemplate;
  }

  public <T> T execute(UUID userId, Supplier<T> action) {
    return userCheckoutLock.execute(userId, () -> transactionTemplate.execute(status -> action.get()));
  }
}
