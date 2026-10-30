package com.ticketbooking.notification.messaging;

import com.ticketbooking.notification.config.RabbitConfig;
import com.ticketbooking.notification.model.EmailJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 3: RabbitMQ workers send the email. Failures are retried (see spring.rabbitmq.listener.simple.retry)
 * and then routed to email.jobs.dlq for inspection instead of being lost.
 * Concurrency is set in application.yml (3-5 consumers).
 */
@Slf4j
@Component
public class EmailWorker {

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void send(EmailJob job) {
        // Plug in JavaMailSender / AWS SES / SendGrid here.
        log.info("[EMAIL] to={} subject=\"{}\" body=\"{}\"", job.to(), job.subject(), job.body());
    }
}
