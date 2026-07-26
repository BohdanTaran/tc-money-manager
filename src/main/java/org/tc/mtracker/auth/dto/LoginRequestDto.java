package org.tc.mtracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Login user request")
public record LoginRequestDto(
        @Schema(description = "User's email address", example = "example@mail.com")
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Schema(
                description = "User's password (min 8 chars).",
                example = "Example8",
                format = "password"
        )
        @NotBlank
        @Length(min = 8, max = 72)
        String password
) {
}
