package com.ticketbooking.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.common.events.BookingCancelledEvent;
import com.ticketbooking.common.events.BookingConfirmedEvent;
import com.ticketbooking.common.events.Topics;
import com.ticketbooking.notification.model.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Step 1 of the notification pipeline: turn Kafka domain events into a single SNS publish.
 * SNS then fans the message out to every subscribed channel queue (email, SMS, ... add push later
 * by subscribing one more queue, with zero code changes here).
 */
@Slf4j
@Component
public class BookingEventsListener {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public BookingEventsListener(SnsClient snsClient, ObjectMapper objectMapper,
                                 @Value("${notification.sns-topic-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    @KafkaListener(topics = Topics.BOOKING_CONFIRMED, groupId = "notification-service")
    public void onConfirmed(BookingConfirmedEvent e) throws JsonProcessingException {
        publish(new NotificationMessage("BOOKING_CONFIRMED", e.bookingRef(), e.userId(), e.userEmail(),
                "Your tickets are confirmed!",
                "Booking %s confirmed. Seats %s for show #%d. Amount paid: Rs %s."
                        .formatted(e.bookingRef(), String.join(", ", e.seatNumbers()), e.showId(), e.amount())));
    }

    @KafkaListener(topics = Topics.BOOKING_CANCELLED, groupId = "notification-service")
    public void onCancelled(BookingCancelledEvent e) throws JsonProcessingException {
        publish(new NotificationMessage("BOOKING_CANCELLED", e.bookingRef(), e.userId(), e.userEmail(),
                "Your booking could not be completed",
                "Booking %s was cancelled: %s. Any amount charged will be refunded."
                        .formatted(e.bookingRef(), e.reason())));
    }

    private void publish(NotificationMessage message) throws JsonProcessingException {
        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject(message.subject())
                .message(objectMapper.writeValueAsString(message))
                .build());
        log.info("Published {} for {} to SNS", message.type(), message.bookingRef());
    }
}
