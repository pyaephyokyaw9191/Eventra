package com.cedric.Eventra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "reviews")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Float rating; // 1.0 to 5.0

    @Column(length = 1000)
    private String comment;

    @ManyToOne
    @JoinColumn(name="reviewer_id")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name="provider_id")
    private ServiceProviderProfile provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_service_id") // Can be nullable if some reviews are for provider in general
    private OfferedService offeredService; // The specific service being reviewed

    private LocalDateTime createdAt = LocalDateTime.now();
}
