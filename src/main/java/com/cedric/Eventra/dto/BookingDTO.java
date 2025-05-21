package com.cedric.Eventra.dto;

import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDTO {

    private Long id;
    private String requestName;
    private String description;
    private String location;
    private LocalDate preferredDate;
    private LocalTime preferredTime;
    private String bookingReference;
    private UserDTO user;
    private OfferedServiceDTO service;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private BigDecimal price;

}
