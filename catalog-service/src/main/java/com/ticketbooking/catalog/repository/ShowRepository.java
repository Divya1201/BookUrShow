package com.ticketbooking.catalog.repository;

import com.ticketbooking.catalog.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieIdOrderByStartTime(Long movieId);
}
