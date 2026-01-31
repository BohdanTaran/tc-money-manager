package org.tc.mtracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User sign up response")
public record AuthResponseDTO(
        @Schema(description = "User's id", example = "123123")
        Long id,

        @Schema(description = "User's full name", example = "Abraham Lincoln")
        String fullName,

        @Schema(description = "User's email", examples = "example@mail.com")
        String email,

        @Schema(description = "User's main currency code", example = "USD")
        String currencyCode,

        @Schema(description = "User's activation status", example = "false")
        boolean isActivated
) {
}
