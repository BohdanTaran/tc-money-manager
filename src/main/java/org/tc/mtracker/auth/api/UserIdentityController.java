package org.tc.mtracker.auth.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tc.mtracker.auth.dto.UpdateEmailRequestDto;
import org.tc.mtracker.auth.dto.UpdatePasswordRequestDto;
import org.tc.mtracker.auth.service.EmailVerificationService;
import org.tc.mtracker.auth.service.PasswordManagementService;

@RestController
@RequiredArgsConstructor
@Validated
public class UserIdentityController implements UserIdentityApi {

    private final EmailVerificationService emailVerificationService;
    private final PasswordManagementService passwordManagementService;

    @Override
    public ResponseEntity<Void> updateEmail(
            UpdateEmailRequestDto dto,
            Authentication auth
    ) {
        emailVerificationService.updateEmail(dto, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updatePassword(
            UpdatePasswordRequestDto dto,
            Authentication auth
    ) {
        passwordManagementService.updatePassword(dto, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmailUpdate(token);
        return ResponseEntity.ok().build();
    }
}
