package com.ticketbooking.booking.service;

import com.ticketbooking.booking.client.CatalogClient;
import com.ticketbooking.booking.client.CatalogClient.SeatInfo;
import com.ticketbooking.booking.client.CatalogClient.ShowSeats;
import com.ticketbooking.booking.dto.BookingDtos.BookingResponse;
import com.ticketbooking.booking.dto.BookingDtos.CreateBookingRequest;
import com.ticketbooking.booking.model.Booking;
import com.ticketbooking.booking.model.BookingStatus;
import com.ticketbooking.booking.repository.BookingRepository;
import com.ticketbooking.common.events.BookingCancelledEvent;
import com.ticketbooking.common.events.BookingConfirmedEvent;
import com.ticketbooking.common.events.BookingCreatedEvent;
import com.ticketbooking.common.events.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the booking Saga:
 * <pre>
 *  create()  -> hold seats in Redis -> save PENDING -> event booking-created
 *  payment-service -> event payment-completed
 *  onPaymentCompleted(SUCCESS) -> CONFIRMED -> event booking-confirmed (catalog marks seats BOOKED)
 *  onPaymentCompleted(FAILED)  -> CANCELLED -> release seats (compensation) -> event booking-cancelled
 *  expirePendingBookings()     -> EXPIRED   -> release seats
 * </pre>
 * Events are handed to Spring's ApplicationEventPublisher and only sent to Kafka after the DB
 * transaction commits (see KafkaEventPublisher), so consumers never see a booking that doesn't exist yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final CatalogClient catalogClient;
    private final ApplicationEventPublisher events;

    @Transactional
    public BookingResponse create(Long userId, String userEmail, CreateBookingRequest request) {
        List<String> seats = request.seatNumbers().stream().map(String::toUpperCase).distinct().toList();
        if (seats.size() != request.seatNumbers().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate seat numbers in request");
        }

        // 1. Validate against the catalog (seat exists and is not already BOOKED)
        ShowSeats showSeats = catalogClient.getShowSeats(request.showId());
        Map<String, SeatInfo> bySeat = showSeats.seats().stream()
                .collect(Collectors.toMap(SeatInfo::seatNumber, Function.identity()));
        for (String seat : seats) {
            SeatInfo info = bySeat.get(seat);
            if (info == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat " + seat + " does not exist");
            }
            if (!"AVAILABLE".equals(info.status())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat " + seat + " is already booked");
            }
        }

        // 2. Hold the seats atomically in Redis (all-or-nothing)
        String bookingRef = UUID.randomUUID().toString();
        if (!seatLockService.lockSeats(request.showId(), seats, bookingRef)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "One or more seats are currently held by another user. Please pick different seats.");
        }

        // 3. Persist the PENDING booking. If that fails, give the seats back.
        try {
            Booking booking = new Booking();
            booking.setBookingRef(bookingRef);
            booking.setUserId(userId);
            booking.setUserEmail(userEmail);
            booking.setShowId(request.showId());
            booking.setSeatNumbers(new ArrayList<>(seats));
            booking.setAmount(showSeats.seatPrice().multiply(BigDecimal.valueOf(seats.size())));
            booking.setStatus(BookingStatus.PENDING);
            booking.setExpiresAt(LocalDateTime.now().plus(seatLockService.holdDuration()));
            bookingRepository.save(booking);

            // 4. Ask payment-service to charge (sent to Kafka after commit)
            events.publishEvent(new BookingCreatedEvent(bookingRef, userId, userEmail, request.showId(),
                    seats, booking.getAmount()));
            log.info("Booking {} created: show {}, seats {}", bookingRef, request.showId(), seats);
            return BookingResponse.from(booking);
        } catch (RuntimeException e) {
            seatLockService.releaseSeats(request.showId(), seats, bookingRef);
            throw e;
        }
    }

    /** Kafka may deliver the same event more than once, so this is idempotent: only PENDING bookings change. */
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        Booking booking = bookingRepository.findByBookingRef(event.bookingRef()).orElse(null);
        if (booking == null) {
            log.warn("Payment result for unknown booking {}", event.bookingRef());
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            // e.g. booking EXPIRED before the payment came back -> in production you'd trigger a refund here
            log.warn("Ignoring payment {} for booking {} in status {}", event.status(), booking.getBookingRef(),
                    booking.getStatus());
            return;
        }

        if (event.status() == PaymentCompletedEvent.PaymentStatus.SUCCESS) {
            booking.setStatus(BookingStatus.CONFIRMED);
            // Seat locks are NOT released here: they expire on their own TTL, which covers the short window
            // before catalog-service marks the seats BOOKED from the booking-confirmed event.
            events.publishEvent(new BookingConfirmedEvent(booking.getBookingRef(), booking.getUserId(),
                    booking.getUserEmail(), booking.getShowId(), List.copyOf(booking.getSeatNumbers()),
                    booking.getAmount()));
            log.info("Booking {} CONFIRMED", booking.getBookingRef());
        } else {
            cancel(booking, BookingStatus.CANCELLED, "Payment failed: " + event.failureReason());
        }
    }

    /** Runs every minute: bookings whose payment never completed give their seats back. */
    @Transactional
    public int expirePendingBookings() {
        List<Booking> stale = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING,
                LocalDateTime.now());
        stale.forEach(b -> cancel(b, BookingStatus.EXPIRED, "Payment not completed in time"));
        return stale.size();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long userId, String bookingRef) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .filter(b -> b.getUserId().equals(userId)) // users can only see their own bookings
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /** Saga compensation step. */
    private void cancel(Booking booking, BookingStatus status, String reason) {
        booking.setStatus(status);
        booking.setFailureReason(reason);
        seatLockService.releaseSeats(booking.getShowId(), List.copyOf(booking.getSeatNumbers()),
                booking.getBookingRef());
        events.publishEvent(new BookingCancelledEvent(booking.getBookingRef(), booking.getUserId(),
                booking.getUserEmail(), reason));
        log.info("Booking {} {}: {}", booking.getBookingRef(), status, reason);
    }
}
