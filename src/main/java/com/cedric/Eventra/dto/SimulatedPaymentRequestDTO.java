package com.cedric.Eventra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SimulatedPaymentRequestDTO {

    @NotBlank(message = "Card number is required.")
    @Size(min = 16, max = 16, message = "Card number must be 16 digits.")
    @Pattern(regexp = "\\d+", message = "Card number must contain only digits.")
    private String dummyCardNumber;

    @NotBlank(message = "Expiry date is required (MM/YY).")
    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/([0-9]{2})$", message = "Expiry date must be in MM/YY format.")
    private String dummyExpiryDate;

    @NotBlank(message = "CVV is required.")
    @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits.")
    @Pattern(regexp = "\\d+", message = "CVV must contain only digits.")
    private String dummyCvv;

}