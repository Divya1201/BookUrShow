package com.ticketbooking.catalog.controller;

import com.ticketbooking.catalog.dto.CatalogDtos.MovieDto;
import com.ticketbooking.catalog.dto.CatalogDtos.ShowDto;
import com.ticketbooking.catalog.dto.CatalogDtos.ShowSeatsDto;
import com.ticketbooking.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/api/movies")
    public List<MovieDto> movies() {
        return catalogService.listMovies();
    }

    @GetMapping("/api/movies/{movieId}/shows")
    public List<ShowDto> shows(@PathVariable Long movieId) {
        return catalogService.listShows(movieId);
    }

    /** Public for users, and also called service-to-service by booking-service to validate a request. */
    @GetMapping("/api/shows/{showId}/seats")
    public ShowSeatsDto seats(@PathVariable Long showId) {
        return catalogService.showSeats(showId);
    }
}
