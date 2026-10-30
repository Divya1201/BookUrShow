package com.ticketbooking.payment.repository;

import com.ticketbooking.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingRef(String bookingRef);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
