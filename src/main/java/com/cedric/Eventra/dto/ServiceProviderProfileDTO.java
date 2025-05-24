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
public class ServiceProviderProfileDTO {
//    private Long userId;
    private String location;
    private String postcode;
    private String profilePictureUrl;
    private String coverPhotoUrl;
    private ServiceCategory serviceCategory;
    // add ons
    private String serviceName;
    private String abn;

    // From user
    private String userEmail;
    private String userFirstName;
    private String userLastName;

    // From reviews
    private Float averageRating;
    private Integer totalReviews;
}

/*
Try to use this explicit mapping if needed to use userid;
@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Avoid the ambiguity manually
        modelMapper.typeMap(User.class, UserDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getServiceProviderProfile(), UserDTO::setServiceProviderProfile);
        });

        modelMapper.typeMap(ServiceProviderProfile.class, ServiceProviderProfileDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getUser().getId(), ServiceProviderProfileDTO::setUserId);
        });

        return modelMapper;
    }
}
 */
