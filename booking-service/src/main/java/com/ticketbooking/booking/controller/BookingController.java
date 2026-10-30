package com.ticketbooking.booking.controller;

import com.ticketbooking.booking.dto.BookingDtos.BookingResponse;
import com.ticketbooking.booking.dto.BookingDtos.CreateBookingRequest;
import com.ticketbooking.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** X-User-Id / X-User-Email are injected by the API gateway after JWT validation. */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED) // accepted: payment happens asynchronously
    public BookingResponse create(@RequestHeader("X-User-Id") Long userId,
                                  @RequestHeader("X-User-Email") String userEmail,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(userId, userEmail, request);
    }

    @GetMapping("/{bookingRef}")
    public BookingResponse get(@RequestHeader("X-User-Id") Long userId, @PathVariable String bookingRef) {
        return bookingService.get(userId, bookingRef);
    }

    @GetMapping
    public List<BookingResponse> myBookings(@RequestHeader("X-User-Id") Long userId) {
        return bookingService.listForUser(userId);
    }
}
