package com.cedric.Eventra.entity;

import com.cedric.Eventra.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "offered_services")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferedService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String name;
    private BigDecimal price;
    private Boolean available;

    @ManyToOne
    @JoinColumn(name="provider_id")
    private User provider;

    private String location;       // Optional, for filtering
}
