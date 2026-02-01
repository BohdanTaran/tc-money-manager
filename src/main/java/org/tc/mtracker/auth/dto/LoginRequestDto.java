package org.tc.mtracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Login user request")
public record LoginRequestDto(
        @Schema(description = "User's email address", example = "example@mail.com")
        @NotBlank @Email String email,

        @Schema(
                description = "User's password (min 8 chars).",
                example = "Example8",
                format = "password"
        )
        @NotBlank @Length(min = 8, max = 256) String password
) {
}
