package org.tc.mtracker.dto;

public record UserSignedUpResponseDTO(
        Long id,
        String fullName,
        String email,
        String currencyCode,
        boolean isActivated
) {
}
