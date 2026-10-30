package com.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Note: with several booking-service replicas each one runs this job. That's safe because the
 * transition only touches PENDING rows and @Version gives optimistic locking; for heavier jobs use ShedLock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${booking.expiry-check-interval:PT1M}")
    public void expireStaleBookings() {
        try {
            int expired = bookingService.expirePendingBookings();
            if (expired > 0) {
                log.info("Expired {} unpaid booking(s)", expired);
            }
        } catch (RuntimeException e) {
            log.warn("Expiry run failed, will retry next interval: {}", e.getMessage());
        }
    }
}
