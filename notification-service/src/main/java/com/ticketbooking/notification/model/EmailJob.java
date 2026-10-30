package com.ticketbooking.notification.model;

/** Unit of work for the RabbitMQ email worker pool. */
public record EmailJob(String to, String subject, String body, String bookingRef) {
}
