package com.ticketbooking.notification.model;

/** What gets published to the SNS topic and fanned out to every channel queue. */
public record NotificationMessage(
        String type,        // BOOKING_CONFIRMED | BOOKING_CANCELLED
        String bookingRef,
        Long userId,
        String email,
        String subject,
        String text) {
}
