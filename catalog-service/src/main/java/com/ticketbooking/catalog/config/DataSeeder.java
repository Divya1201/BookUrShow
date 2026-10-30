package com.ticketbooking.catalog.config;

import com.ticketbooking.catalog.model.Movie;
import com.ticketbooking.catalog.model.Seat;
import com.ticketbooking.catalog.model.Show;
import com.ticketbooking.catalog.repository.MovieRepository;
import com.ticketbooking.catalog.repository.SeatRepository;
import com.ticketbooking.catalog.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Inserts demo movies, shows and seats (rows A-B, seats 1-10) on first start. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            return;
        }
        Movie m1 = movieRepository.save(new Movie("Interstellar", "English", "Sci-Fi", 169));
        Movie m2 = movieRepository.save(new Movie("3 Idiots", "Hindi", "Comedy", 170));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Show> shows = showRepository.saveAll(List.of(
                new Show(m1, "PVR Select City", "Delhi", tomorrow.atTime(LocalTime.of(18, 0)), new BigDecimal("350.00")),
                new Show(m1, "INOX Nehru Place", "Delhi", tomorrow.atTime(LocalTime.of(21, 30)), new BigDecimal("300.00")),
                new Show(m2, "PVR Select City", "Delhi", tomorrow.atTime(LocalTime.of(15, 0)), new BigDecimal("250.00"))
        ));

        List<Seat> seats = new ArrayList<>();
        for (Show show : shows) {
            for (String row : List.of("A", "B")) {
                for (int n = 1; n <= 10; n++) {
                    seats.add(new Seat(show.getId(), row + n));
                }
            }
        }
        seatRepository.saveAll(seats);
        log.info("Seeded {} movies, {} shows, {} seats", 2, shows.size(), seats.size());
    }
}
