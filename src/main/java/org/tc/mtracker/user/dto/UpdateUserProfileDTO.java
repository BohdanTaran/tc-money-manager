package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

public record UpdateUserProfileDTO(
        @Schema(description = "User's full name", example = "Abraham Lincoln")
        @Length(min = 1, max = 128)
        String fullName
) {
}
