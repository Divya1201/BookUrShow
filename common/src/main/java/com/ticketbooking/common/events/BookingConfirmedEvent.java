package com.ticketbooking.common.events;

import java.math.BigDecimal;
import java.util.List;

/** Published by booking-service after a successful payment. Catalog marks seats booked; notification informs the user. */
public record BookingConfirmedEvent(
        String bookingRef,
        Long userId,
        String userEmail,
        Long showId,
        List<String> seatNumbers,
        BigDecimal amount) {
}
