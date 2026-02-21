package org.tc.mtracker.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtPurpose {
    EMAIL_VERIFICATION("email_verification"),
    PASSWORD_RESET("password_reset"),
    EMAIL_UPDATE_VERIFICATION("email_update_verification");

    private final String value;
}
