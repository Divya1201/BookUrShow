package com.ticketbooking.booking.model;

/**
 * PENDING  -> seats held in Redis, waiting for payment
 * CONFIRMED -> payment succeeded
 * CANCELLED -> payment failed (Saga compensation released the seats)
 * EXPIRED   -> no payment result within the hold window
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
