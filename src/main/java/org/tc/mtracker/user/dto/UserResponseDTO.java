package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.tc.mtracker.currency.CurrencyCode;

@Schema(description = "Get user info")
public record UserResponseDTO(
        @Schema(description = "User's id", example = "123")
        Long id,

        @Schema(description = "User's email address", example = "example@mail.com")
        String email,

        @Schema(description = "User's full name", example = "Abraham Lincoln")
        String fullName,

        @Schema(description = "User's main currency (ISO 4217)", example = "USD")
        CurrencyCode currencyCode,

        @Schema(description = "User's avatar url", example = "https://example.com/avatar.jpg")
        String avatarUrl,

        @Schema(description = "User's activation status", example = "false")
        boolean isActivated
) {
}
