package com.ticketbooking.booking.messaging;

import com.ticketbooking.booking.service.BookingService;
import com.ticketbooking.common.events.PaymentCompletedEvent;
import com.ticketbooking.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private final BookingService bookingService;

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "booking-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        bookingService.onPaymentCompleted(event);
    }
}
