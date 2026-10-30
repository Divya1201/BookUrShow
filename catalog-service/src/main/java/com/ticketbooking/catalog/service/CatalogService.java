package com.ticketbooking.catalog.service;

import com.ticketbooking.catalog.dto.CatalogDtos.MovieDto;
import com.ticketbooking.catalog.dto.CatalogDtos.SeatDto;
import com.ticketbooking.catalog.dto.CatalogDtos.ShowDto;
import com.ticketbooking.catalog.dto.CatalogDtos.ShowSeatsDto;
import com.ticketbooking.catalog.model.Seat;
import com.ticketbooking.catalog.model.Show;
import com.ticketbooking.catalog.repository.MovieRepository;
import com.ticketbooking.catalog.repository.SeatRepository;
import com.ticketbooking.catalog.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    public static final String MOVIES_CACHE = "movies";
    public static final String SHOWS_CACHE = "shows-by-movie";
    public static final String SEATS_CACHE = "seats-by-show";

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    /** Read-heavy and rarely changes: perfect cache candidate. */
    @Cacheable(MOVIES_CACHE)
    @Transactional(readOnly = true)
    public List<MovieDto> listMovies() {
        log.info("Cache miss: loading movies from MySQL");
        return movieRepository.findAll().stream()
                .map(m -> new MovieDto(m.getId(), m.getTitle(), m.getLanguage(), m.getGenre(), m.getDurationMinutes()))
                .toList();
    }

    @Cacheable(value = SHOWS_CACHE, key = "#movieId")
    @Transactional(readOnly = true)
    public List<ShowDto> listShows(Long movieId) {
        log.info("Cache miss: loading shows for movie {}", movieId);
        return showRepository.findByMovieIdOrderByStartTime(movieId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Cached briefly; evicted whenever seats in this show get booked. */
    @Cacheable(value = SEATS_CACHE, key = "#showId")
    @Transactional(readOnly = true)
    public ShowSeatsDto showSeats(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
        List<SeatDto> seats = seatRepository.findByShowIdOrderBySeatNumber(showId).stream()
                .map(s -> new SeatDto(s.getSeatNumber(), s.getStatus().name()))
                .toList();
        return new ShowSeatsDto(show.getId(), show.getSeatPrice(), seats);
    }

    /** Called from the Kafka listener once payment succeeded. Idempotent: replaying the event is harmless. */
    @CacheEvict(value = SEATS_CACHE, key = "#showId")
    @Transactional
    public void markSeatsBooked(Long showId, List<String> seatNumbers, String bookingRef) {
        List<Seat> seats = seatRepository.findByShowIdAndSeatNumberIn(showId, seatNumbers);
        for (Seat seat : seats) {
            if (seat.getStatus() == Seat.Status.BOOKED && !bookingRef.equals(seat.getBookingRef())) {
                // Should never happen thanks to Redis locks; log loudly if it does.
                log.error("Seat {} of show {} already booked by {}", seat.getSeatNumber(), showId, seat.getBookingRef());
                continue;
            }
            seat.setStatus(Seat.Status.BOOKED);
            seat.setBookingRef(bookingRef);
        }
        log.info("Marked seats {} of show {} as BOOKED for booking {}", seatNumbers, showId, bookingRef);
    }

    private ShowDto toDto(Show s) {
        return new ShowDto(s.getId(), s.getMovie().getId(), s.getMovie().getTitle(), s.getTheatreName(),
                s.getCity(), s.getStartTime(), s.getSeatPrice());
    }
}
