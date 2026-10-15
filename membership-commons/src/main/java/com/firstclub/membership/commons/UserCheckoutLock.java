package com.firstclub.membership.commons;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Redis-backed implementation of the shared per-user checkout lock. */
@Component
public class UserCheckoutLock implements RedisLock {
  private static final Duration LOCK_TTL = Duration.ofSeconds(60);
  private static final DefaultRedisScript<Long> RELEASE_IF_OWNER =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else"
              + " return 0 end",
          Long.class);
  private static final DefaultRedisScript<Long> RENEW_IF_OWNER =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1],"
              + " ARGV[2]) else return 0 end",
          Long.class);
  private final StringRedisTemplate redisTemplate;
  private final ScheduledExecutorService renewalExecutor;

  public UserCheckoutLock(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
    this.renewalExecutor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "membership-checkout-lock-renewer");
              thread.setDaemon(true);
              return thread;
            });
  }

  @Override
  public <T> T execute(UUID userId, Supplier<T> action) {
    String key = "membership:checkout:user:" + userId;
    String token = UUID.randomUUID().toString();
    if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL))) {
      throw new ConflictException("A membership checkout is already in progress for this user");
    }
    ScheduledFuture<?> renewal =
        renewalExecutor.scheduleAtFixedRate(
            () -> renew(key, token),
            LOCK_TTL.toSeconds() / 3,
            LOCK_TTL.toSeconds() / 3,
            TimeUnit.SECONDS);
    try {
      return action.get();
    } finally {
      renewal.cancel(false);
      redisTemplate.execute(RELEASE_IF_OWNER, List.of(key), token);
    }
  }

  private void renew(String key, String token) {
    try {
      redisTemplate.execute(
          RENEW_IF_OWNER, List.of(key), token, Long.toString(LOCK_TTL.toMillis()));
    } catch (RuntimeException ignored) {
      // A later owner-token check prevents this worker from deleting another request's lock.
    }
  }

  @PreDestroy
  void shutdownRenewalExecutor() {
    renewalExecutor.shutdownNow();
  }
}
