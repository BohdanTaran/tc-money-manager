package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UserProfileResponseDTO(
        @Schema(description = "User's full name", example = "Abraham Lincoln")
        String fullName,

        @Schema(description = "User's avatar url", example = "https://example.com/avatar.jpg")
        String avatarUrl
) {
}
