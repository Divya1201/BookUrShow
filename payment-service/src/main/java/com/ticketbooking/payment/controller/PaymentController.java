package com.ticketbooking.payment.controller;

import com.ticketbooking.payment.model.Payment;
import com.ticketbooking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    public record PaymentView(String bookingRef, BigDecimal amount, String status, String transactionId,
                              String failureReason, int attempts, LocalDateTime createdAt) {
        static PaymentView from(Payment p) {
            return new PaymentView(p.getBookingRef(), p.getAmount(), p.getStatus().name(), p.getTransactionId(),
                    p.getFailureReason(), p.getAttempts(), p.getCreatedAt());
        }
    }

    private final PaymentService paymentService;

    @GetMapping("/api/payments")
    public List<PaymentView> myPayments(@RequestHeader("X-User-Id") Long userId) {
        return paymentService.listForUser(userId).stream().map(PaymentView::from).toList();
    }
}
