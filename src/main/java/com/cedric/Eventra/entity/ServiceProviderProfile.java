package com.cedric.Eventra.entity;

import com.cedric.Eventra.enums.ServiceCategory;
import com.cedric.Eventra.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "service_provider_profiles")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceProviderProfile {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    private List<Review> reviews;

    private Float averageRating;

    @Enumerated(EnumType.STRING)
    private ServiceCategory serviceCategory;

    private String serviceName;
    private String ABN;

    private String location;
    private String postcode;

    @Column(name = "profile_picture_filename")
    private String profilePictureFilename;

    @Column(name = "cover_photo_filename")
    private String coverPhotoFilename;
}
