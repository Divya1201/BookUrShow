package com.ticketbooking.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for Razorpay/Stripe. Outcomes are random so you can watch every path of the Saga:
 * success, a hard decline (booking cancelled), and a timeout (retried through SQS).
 */
@Slf4j
@Component
public class MockPaymentGateway {

    public static class GatewayTimeoutException extends RuntimeException {
        public GatewayTimeoutException(String message) {
            super(message);
        }
    }

    public record ChargeResult(boolean success, String transactionId, String declineReason) {
    }

    private final double timeoutRate;
    private final double declineRate;

    public MockPaymentGateway(@Value("${payment.mock.timeout-rate:0.15}") double timeoutRate,
                              @Value("${payment.mock.decline-rate:0.10}") double declineRate) {
        this.timeoutRate = timeoutRate;
        this.declineRate = declineRate;
    }

    public ChargeResult charge(String bookingRef, BigDecimal amount) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < timeoutRate) {
            log.warn("Gateway timeout for {}", bookingRef);
            throw new GatewayTimeoutException("Payment gateway did not respond");
        }
        if (roll < timeoutRate + declineRate) {
            return new ChargeResult(false, null, "Card declined by issuer");
        }
        return new ChargeResult(true, "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), null);
    }
}
