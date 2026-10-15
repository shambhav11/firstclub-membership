package com.firstclub.membership.commons;

import java.util.UUID;
import java.util.function.Supplier;

/** Serializes checkout commands for one external user across application pods. */
public interface RedisLock {
  <T> T execute(UUID userId, Supplier<T> action);
}
