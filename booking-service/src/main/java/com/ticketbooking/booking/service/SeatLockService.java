package com.ticketbooking.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Distributed seat holds using Redis.
 *
 * <p>Each seat gets a key {@code seat-lock:{showId}:{seatNumber}} whose value is the bookingRef that owns it.
 * {@code SET NX PX} is atomic, so when two users race for the same seat exactly one wins, no matter how many
 * booking-service replicas are running. The TTL releases abandoned holds automatically.
 */
@Slf4j
@Service
public class SeatLockService {

    /** Delete the key only if we still own it: another booking may have taken the seat after our TTL expired. */
    private static final RedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final Duration holdDuration;

    public SeatLockService(StringRedisTemplate redis,
                           @Value("${booking.seat-hold-duration:PT5M}") Duration holdDuration) {
        this.redis = redis;
        this.holdDuration = holdDuration;
    }

    /**
     * Locks all seats or none. Seats are locked in sorted order so two users requesting
     * overlapping seats always contend on the same seat first.
     */
    public boolean lockSeats(Long showId, List<String> seatNumbers, String bookingRef) {
        List<String> acquired = new ArrayList<>();
        for (String seat : seatNumbers.stream().sorted().toList()) {
            Boolean ok = redis.opsForValue().setIfAbsent(key(showId, seat), bookingRef, holdDuration);
            if (!Boolean.TRUE.equals(ok)) {
                log.info("Seat {} of show {} is already held; rolling back {} lock(s)", seat, showId, acquired.size());
                releaseSeats(showId, acquired, bookingRef);
                return false;
            }
            acquired.add(seat);
        }
        return true;
    }

    public void releaseSeats(Long showId, List<String> seatNumbers, String bookingRef) {
        for (String seat : seatNumbers) {
            redis.execute(RELEASE_IF_OWNER, List.of(key(showId, seat)), bookingRef);
        }
    }

    public Duration holdDuration() {
        return holdDuration;
    }

    static String key(Long showId, String seat) {
        return "seat-lock:" + showId + ":" + seat;
    }
}
