package com.ticketbooking.catalog.messaging;

import com.ticketbooking.catalog.service.CatalogService;
import com.ticketbooking.common.events.BookingConfirmedEvent;
import com.ticketbooking.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventsListener {

    private final CatalogService catalogService;

    @KafkaListener(topics = Topics.BOOKING_CONFIRMED, groupId = "catalog-service")
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        catalogService.markSeatsBooked(event.showId(), event.seatNumbers(), event.bookingRef());
    }
}
