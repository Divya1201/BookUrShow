package com.ticketbooking.payment.service;

import com.ticketbooking.common.events.BookingCreatedEvent;
import com.ticketbooking.common.events.PaymentCompletedEvent;
import com.ticketbooking.common.events.PaymentCompletedEvent.PaymentStatus;
import com.ticketbooking.payment.gateway.MockPaymentGateway;
import com.ticketbooking.payment.gateway.MockPaymentGateway.ChargeResult;
import com.ticketbooking.payment.gateway.MockPaymentGateway.GatewayTimeoutException;
import com.ticketbooking.payment.messaging.PaymentRetryMessage;
import com.ticketbooking.payment.model.Payment;
import com.ticketbooking.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final int maxAttempts;

    public PaymentService(PaymentRepository paymentRepository, MockPaymentGateway gateway,
                          ApplicationEventPublisher events, @Value("${payment.max-attempts:3}") int maxAttempts) {
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
        this.events = events;
        this.maxAttempts = maxAttempts;
    }

    /** First attempt, triggered by the booking-created Kafka event. */
    @Transactional
    public void handleBookingCreated(BookingCreatedEvent event) {
        if (paymentRepository.findByBookingRef(event.bookingRef()).isPresent()) {
            log.info("Duplicate booking-created event for {} ignored (idempotency)", event.bookingRef());
            return;
        }
        Payment payment = new Payment();
        payment.setBookingRef(event.bookingRef());
        payment.setUserId(event.userId());
        payment.setAmount(event.amount());
        payment.setStatus(Payment.Status.PROCESSING);
        paymentRepository.save(payment);
        attempt(payment, event);
    }

    /** Later attempts, triggered by a delayed message on the SQS retry queue. */
    @Transactional
    public void handleRetry(PaymentRetryMessage message) {
        Payment payment = paymentRepository.findByBookingRef(message.event().bookingRef()).orElse(null);
        if (payment == null || payment.getStatus() != Payment.Status.PROCESSING) {
            return; // already finished, nothing to do
        }
        attempt(payment, message.event());
    }

    @Transactional(readOnly = true)
    public List<Payment> listForUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void attempt(Payment payment, BookingCreatedEvent event) {
        payment.setAttempts(payment.getAttempts() + 1);
        try {
            ChargeResult result = gateway.charge(event.bookingRef(), event.amount());
            if (result.success()) {
                payment.setStatus(Payment.Status.SUCCESS);
                payment.setTransactionId(result.transactionId());
                finish(payment, PaymentStatus.SUCCESS);
            } else {
                payment.setStatus(Payment.Status.FAILED);
                payment.setFailureReason(result.declineReason());
                finish(payment, PaymentStatus.FAILED);
            }
        } catch (GatewayTimeoutException e) {
            if (payment.getAttempts() >= maxAttempts) {
                payment.setStatus(Payment.Status.FAILED);
                payment.setFailureReason("Gateway timeout after " + payment.getAttempts() + " attempts");
                finish(payment, PaymentStatus.FAILED);
            } else {
                log.info("Payment {} attempt {} timed out; scheduling retry via SQS", event.bookingRef(),
                        payment.getAttempts());
                events.publishEvent(new PaymentRetryMessage(event, payment.getAttempts()));
            }
        }
    }

    private void finish(Payment payment, PaymentStatus status) {
        log.info("Payment for {} finished: {} after {} attempt(s)", payment.getBookingRef(), status,
                payment.getAttempts());
        events.publishEvent(new PaymentCompletedEvent(payment.getBookingRef(), status,
                payment.getTransactionId(), payment.getFailureReason()));
    }
}
