package com.ticketbooking.booking.messaging;

import com.ticketbooking.common.events.BookingCancelledEvent;
import com.ticketbooking.common.events.BookingConfirmedEvent;
import com.ticketbooking.common.events.BookingCreatedEvent;
import com.ticketbooking.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards domain events to Kafka only AFTER the database transaction commits.
 * Key = bookingRef, so all events of one booking land on the same partition and stay in order.
 *
 * <p>Upgrade idea: replace this with the Transactional Outbox pattern so an event is never lost
 * if the service crashes between the commit and the Kafka send.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCreatedEvent event) {
        send(Topics.BOOKING_CREATED, event.bookingRef(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingConfirmedEvent event) {
        send(Topics.BOOKING_CONFIRMED, event.bookingRef(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCancelledEvent event) {
        send(Topics.BOOKING_CANCELLED, event.bookingRef(), event);
    }

    private void send(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} to {}", key, topic, ex);
            } else {
                log.debug("Published {} to {} partition {}", key, topic, result.getRecordMetadata().partition());
            }
        });
    }
}
