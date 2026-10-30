package com.ticketbooking.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_seat_show_number", columnNames = {"show_id", "seat_number"}))
@Getter
@Setter
@NoArgsConstructor
public class Seat {

    public enum Status { AVAILABLE, BOOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    private String bookingRef;

    /** Optimistic locking: a second concurrent update on the same seat fails instead of silently overwriting. */
    @Version
    private Long version;

    public Seat(Long showId, String seatNumber) {
        this.showId = showId;
        this.seatNumber = seatNumber;
    }
}
