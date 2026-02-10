package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User avatar url response")
public record ResponseUserAvatarUrlDTO(
        @Schema(description = "User's avatar url", examples = "https://example.com/example")
        String avatarUrl
) {
}
