package org.tc.mtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record UserSignUpRequestDTO(
        @NotBlank @Email String email,
        @NotBlank @Length(min = 8, max = 256) String password,
        @NotBlank @Length(min = 1, max = 128) String fullName,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currencyCode
) {
}
