package com.ticketbooking.common.events;

/** Published when a booking fails payment or expires (Saga compensation). */
public record BookingCancelledEvent(
        String bookingRef,
        Long userId,
        String userEmail,
        String reason) {
}
