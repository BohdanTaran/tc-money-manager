package org.tc.mtracker.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT response")
public record JwtResponseDTO(

        @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiJ9.ey...", format = "JWT")
        String accessToken
) {
}
