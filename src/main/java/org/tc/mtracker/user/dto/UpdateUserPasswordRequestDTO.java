package org.tc.mtracker.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UpdateUserPasswordRequestDTO(
        @NotBlank
        String currentPassword,
        @NotBlank @Length(min = 8, max = 256)
        String newPassword,
        @NotBlank
        String confirmPassword
) {
}
