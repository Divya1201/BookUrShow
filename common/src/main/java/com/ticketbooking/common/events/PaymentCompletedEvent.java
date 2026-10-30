package com.ticketbooking.common.events;

/** Published by payment-service once a payment has a final outcome. */
public record PaymentCompletedEvent(
        String bookingRef,
        PaymentStatus status,
        String transactionId,
        String failureReason) {

    public enum PaymentStatus {
        SUCCESS,
        FAILED
    }
}
