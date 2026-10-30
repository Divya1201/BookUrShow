package com.ticketbooking.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.notification.config.RabbitConfig;
import com.ticketbooking.notification.model.EmailJob;
import com.ticketbooking.notification.model.NotificationMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Step 2: each channel has its own SQS queue subscribed to the SNS topic (raw message delivery),
 * so a slow or broken channel never affects the others.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelQueueListeners {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    /** Email is slow and rate-limited by providers, so hand it to a RabbitMQ worker pool. */
    @SqsListener("${notification.email-queue}")
    public void onEmailChannel(String body) throws JsonProcessingException {
        NotificationMessage msg = objectMapper.readValue(body, NotificationMessage.class);
        EmailJob job = new EmailJob(msg.email(), msg.subject(), msg.text(), msg.bookingRef());
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.EMAIL_ROUTING_KEY, job);
        log.info("Queued email job for {} on RabbitMQ", msg.bookingRef());
    }

    @SqsListener("${notification.sms-queue}")
    public void onSmsChannel(String body) throws JsonProcessingException {
        NotificationMessage msg = objectMapper.readValue(body, NotificationMessage.class);
        // Plug in Twilio/MSG91 here. For the demo we just log it.
        log.info("[SMS] user {}: {}", msg.userId(), msg.text());
    }
}
