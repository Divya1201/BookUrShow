package com.ticketbooking.payment.messaging;

import com.ticketbooking.common.events.BookingCreatedEvent;
import com.ticketbooking.common.events.PaymentCompletedEvent;
import com.ticketbooking.common.events.Topics;
import com.ticketbooking.payment.service.PaymentService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * All messaging for payment-service in one place:
 * <ul>
 *   <li>Kafka in:  booking-created     -> start payment</li>
 *   <li>Kafka out: payment-completed   -> booking-service finishes the Saga</li>
 *   <li>SQS:       payment-retry-queue -> delayed retries after gateway timeouts</li>
 * </ul>
 * Why SQS for retries instead of Kafka? SQS supports per-message delays and its own dead-letter queue,
 * so a slow retry never blocks the Kafka partition behind it.
 */
@Slf4j
@Component
public class PaymentMessaging {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SqsTemplate sqsTemplate;
    private final String retryQueue;
    private final int retryDelaySeconds;

    public PaymentMessaging(PaymentService paymentService,
                            KafkaTemplate<String, Object> kafkaTemplate,
                            SqsTemplate sqsTemplate,
                            @Value("${payment.retry-queue}") String retryQueue,
                            @Value("${payment.retry-delay-seconds:10}") int retryDelaySeconds) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.sqsTemplate = sqsTemplate;
        this.retryQueue = retryQueue;
        this.retryDelaySeconds = retryDelaySeconds;
    }

    @KafkaListener(topics = Topics.BOOKING_CREATED, groupId = "payment-service")
    public void onBookingCreated(BookingCreatedEvent event) {
        paymentService.handleBookingCreated(event);
    }

    /** Spring Cloud AWS converts the JSON body back into the record using the type header SqsTemplate sets. */
    @SqsListener("${payment.retry-queue}")
    public void onRetry(PaymentRetryMessage message) {
        log.info("Retrying payment for {} (attempts so far: {})", message.event().bookingRef(),
                message.attemptsSoFar());
        paymentService.handleRetry(message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishResult(PaymentCompletedEvent event) {
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, event.bookingRef(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void scheduleRetry(PaymentRetryMessage message) {
        // delaySeconds = back-off: SQS hides the message until the delay passes
        sqsTemplate.send(to -> to.queue(retryQueue).payload(message).delaySeconds(retryDelaySeconds));
    }
}
