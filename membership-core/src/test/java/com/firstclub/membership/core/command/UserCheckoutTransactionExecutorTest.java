package com.firstclub.membership.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firstclub.membership.commons.RedisLock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class UserCheckoutTransactionExecutorTest {
  private final RedisLock checkoutLock = mock(RedisLock.class);
  private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
  private final UserCheckoutTransactionExecutor executor =
      new UserCheckoutTransactionExecutor(checkoutLock, new TransactionTemplate(transactionManager));

  @Test
  void commitsTheActionBeforeReleasingTheCheckoutLock() {
    UUID userId = UUID.randomUUID();
    runLockedActionImmediately();

    assertEquals("done", executor.execute(userId, () -> "done"));

    assertEquals(1, transactionManager.commits);
    assertEquals(0, transactionManager.rollbacks);
    verify(checkoutLock).execute(eq(userId), any());
  }

  @Test
  void rollsBackWhenTheCheckoutActionFails() {
    UUID userId = UUID.randomUUID();
    runLockedActionImmediately();

    assertThrows(IllegalStateException.class, () -> executor.execute(userId, () -> {
      throw new IllegalStateException("gateway callback failed");
    }));

    assertEquals(0, transactionManager.commits);
    assertEquals(1, transactionManager.rollbacks);
    verify(checkoutLock).execute(eq(userId), any());
  }

  @SuppressWarnings("unchecked")
  private void runLockedActionImmediately() {
    when(checkoutLock.execute(any(), any()))
        .thenAnswer(invocation -> ((java.util.function.Supplier<Object>) invocation.getArgument(1)).get());
  }

  private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
    private int commits;
    private int rollbacks;

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
      commits++;
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
      rollbacks++;
    }
  }
}
