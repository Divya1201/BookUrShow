package com.ticketbooking.catalog.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs returned by the API. They are Serializable because Spring's Redis cache stores them
 * with JDK serialization (simple and safe for records; no Jackson type-info pitfalls).
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record MovieDto(Long id, String title, String language, String genre, int durationMinutes)
            implements Serializable {
    }

    public record ShowDto(Long id, Long movieId, String movieTitle, String theatreName, String city,
                          LocalDateTime startTime, BigDecimal seatPrice) implements Serializable {
    }

    public record SeatDto(String seatNumber, String status) implements Serializable {
    }

    public record ShowSeatsDto(Long showId, BigDecimal seatPrice, List<SeatDto> seats) implements Serializable {
    }
}
