package com.ticketbooking.booking.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/** Synchronous call to catalog-service to validate the show, seats and price before holding seats. */
@Component
public class CatalogClient {

    public record SeatInfo(String seatNumber, String status) {
    }

    public record ShowSeats(Long showId, BigDecimal seatPrice, List<SeatInfo> seats) {
    }

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder, @Value("${services.catalog.url}") String catalogUrl) {
        this.restClient = builder.baseUrl(catalogUrl).build();
    }

    public ShowSeats getShowSeats(Long showId) {
        try {
            return restClient.get()
                    .uri("/api/shows/{id}/seats", showId)
                    .retrieve()
                    .body(ShowSeats.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catalog service unavailable", e);
        }
    }
}
