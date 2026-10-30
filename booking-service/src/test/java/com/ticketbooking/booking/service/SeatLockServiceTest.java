package com.ticketbooking.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SeatLockServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private SeatLockService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new SeatLockService(redis, Duration.ofMinutes(5));
    }

    @Test
    void locksAllSeatsWhenAllAreFree() {
        when(ops.setIfAbsent(anyString(), eq("ref-1"), any(Duration.class))).thenReturn(true);

        boolean locked = service.lockSeats(1L, List.of("A2", "A1"), "ref-1");

        assertThat(locked).isTrue();
        verify(ops).setIfAbsent("seat-lock:1:A1", "ref-1", Duration.ofMinutes(5));
        verify(ops).setIfAbsent("seat-lock:1:A2", "ref-1", Duration.ofMinutes(5));
        verify(redis, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void rollsBackAcquiredLocksWhenOneSeatIsTaken() {
        when(ops.setIfAbsent(eq("seat-lock:1:A1"), anyString(), any(Duration.class))).thenReturn(true);
        when(ops.setIfAbsent(eq("seat-lock:1:A2"), anyString(), any(Duration.class))).thenReturn(false);

        boolean locked = service.lockSeats(1L, List.of("A1", "A2"), "ref-2");

        assertThat(locked).isFalse();
        // A1 was acquired, so it must be released again (only if we still own it)
        verify(redis).execute(any(RedisScript.class), eq(List.of("seat-lock:1:A1")), eq("ref-2"));
    }
}
