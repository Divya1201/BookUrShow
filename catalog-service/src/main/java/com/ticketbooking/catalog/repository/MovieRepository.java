package com.ticketbooking.catalog.repository;

import com.ticketbooking.catalog.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
