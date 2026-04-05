package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.tc.mtracker.currency.CurrencyCode;

public record RequestUpdateUserProfileDTO(
        @Schema(description = "User's full name", example = "Abraham Lincoln")
        @NotBlank @Length(min = 3, max = 35)
        @Pattern(regexp = "^[A-Za-zА-ЯІЄЇҐа-яієїґ'’ʼ]([A-Za-zА-ЯІЄЇҐа-яієїґ'’ʼ\\s-]*[A-Za-zА-ЯІЄЇҐа-яієїґ'’ʼ])?$", message = "Invalid full name")
        String fullName,

        @Schema(description = "User's main currency (ISO 4217)", example = "USD")
        CurrencyCode currencyCode
) {
}
