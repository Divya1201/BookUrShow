package com.ticketbooking.common.events;

/** Kafka topic names, kept in one place so producers and consumers never drift apart. */
public final class Topics {
    public static final String BOOKING_CREATED = "booking-created";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String BOOKING_CONFIRMED = "booking-confirmed";
    public static final String BOOKING_CANCELLED = "booking-cancelled";

    private Topics() {
    }
}
