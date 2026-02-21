package org.tc.mtracker.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.tc.mtracker.currency.CurrencyCode;

@Schema(description = "Get user info")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    @Schema(description = "User's id", example = "123")
    private Long id;

    @Schema(description = "User's email address", example = "example@mail.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "User's full name", example = "Abraham Lincoln")
    @NotBlank
    @Length(min = 1, max = 128)
    private String fullName;

    @Schema(description = "User's main currency (ISO 4217)", example = "USD")
    @NotNull
    private CurrencyCode currencyCode;

    @Schema(description = "User's avatar url", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "User's activation status", example = "false")
    private boolean isActivated;
}
