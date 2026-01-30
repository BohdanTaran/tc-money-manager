package org.tc.mtracker.dto;

import jakarta.validation.constraints.Email;
import org.hibernate.validator.constraints.Length;

public record UserSignUpRequestDTO(
        @Email String email,
        @Length(min = 8, max = 256) String password,
        @Length(min = 1, max = 256) String fullName,
        @Length(min = 3, max = 3) String currencyCode
) {
}
