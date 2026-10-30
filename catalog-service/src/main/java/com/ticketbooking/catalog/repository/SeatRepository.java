package com.ticketbooking.catalog.repository;

import com.ticketbooking.catalog.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByShowIdOrderBySeatNumber(Long showId);

    List<Seat> findByShowIdAndSeatNumberIn(Long showId, Collection<String> seatNumbers);
}
