package com.ticketbooking.common.events;

import java.math.BigDecimal;
import java.util.List;

/** Published by booking-service when seats are held and the booking waits for payment. */
public record BookingCreatedEvent(
        String bookingRef,
        Long userId,
        String userEmail,
        Long showId,
        List<String> seatNumbers,
        BigDecimal amount) {
}
