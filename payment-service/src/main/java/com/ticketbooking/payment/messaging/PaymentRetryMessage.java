package com.ticketbooking.payment.messaging;

import com.ticketbooking.common.events.BookingCreatedEvent;

/** Body of a message on the SQS payment retry queue. */
public record PaymentRetryMessage(BookingCreatedEvent event, int attemptsSoFar) {
}
