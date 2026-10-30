package com.ticketbooking.booking.dto;

import com.ticketbooking.booking.model.Booking;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record CreateBookingRequest(
            @NotNull Long showId,
            @NotEmpty @Size(max = 6, message = "You can book at most 6 seats at once") List<String> seatNumbers) {
    }

    public record BookingResponse(
            String bookingRef,
            Long showId,
            List<String> seatNumbers,
            BigDecimal amount,
            String status,
            String failureReason,
            LocalDateTime expiresAt,
            LocalDateTime createdAt) {

        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getBookingRef(), b.getShowId(), List.copyOf(b.getSeatNumbers()),
                    b.getAmount(), b.getStatus().name(), b.getFailureReason(), b.getExpiresAt(), b.getCreatedAt());
        }
    }
}
