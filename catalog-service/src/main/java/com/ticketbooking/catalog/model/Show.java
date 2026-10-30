package com.ticketbooking.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shows", indexes = @Index(name = "idx_shows_movie", columnList = "movie_id"))
@Getter
@Setter
@NoArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal seatPrice;

    public Show(Movie movie, String theatreName, String city, LocalDateTime startTime, BigDecimal seatPrice) {
        this.movie = movie;
        this.theatreName = theatreName;
        this.city = city;
        this.startTime = startTime;
        this.seatPrice = seatPrice;
    }
}
