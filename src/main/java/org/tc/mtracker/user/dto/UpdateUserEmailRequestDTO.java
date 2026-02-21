package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Update user email request")
public record UpdateUserEmailRequestDTO(
        @Schema(description = "New email address", example = "email@example.com")
        String email
) {
}
