package com.cedric.Eventra.dto;

import com.cedric.Eventra.enums.ServiceCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferedServiceDTO {

    private Long id;
    private String description;
    private String name;
    private BigDecimal price;
    private Boolean available;
    private UserDTO provider;
//    private String location;
}
