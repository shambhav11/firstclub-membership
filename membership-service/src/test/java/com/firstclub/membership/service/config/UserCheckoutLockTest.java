package com.firstclub.membership.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.UserCheckoutLock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UserCheckoutLockTest {
  private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> values = mock(ValueOperations.class);

  private final UserCheckoutLock lock = new UserCheckoutLock(redisTemplate);

  @Test
  void acquiresAndReleasesTheUserCheckoutLock() {
    UUID userId = UUID.randomUUID();
    when(redisTemplate.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

    assertEquals("created", lock.execute(userId, () -> "created"));

    verify(values).setIfAbsent(eq("membership:checkout:user:" + userId), anyString(), any());
    verify(redisTemplate)
        .execute(any(), eq(java.util.List.of("membership:checkout:user:" + userId)), anyString());
  }

  @Test
  void rejectsCheckoutWhenTheUserLockIsAlreadyHeld() {
    when(redisTemplate.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

    assertThrows(
        ConflictException.class, () -> lock.execute(UUID.randomUUID(), () -> "should not run"));
  }
}
