package org.tc.mtracker.dto;

public record UserSignUpResponseDTO(
        Long id,
        String fullName,
        String email,
        String currencyCode,
        boolean isActivated
) {
}
