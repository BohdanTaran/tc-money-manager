package org.tc.mtracker.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tc.mtracker.auth.dto.RequestUpdateUserEmailDTO;
import org.tc.mtracker.auth.dto.RequestUpdateUserPasswordDTO;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Identity", description = "Email and password management endpoints")
@Validated
public class UserIdentityController {

    private final EmailVerificationService emailVerificationService;
    private final PasswordService passwordService;

    @PostMapping("/me/update-email")
    public ResponseEntity<Void> updateEmail(
            @RequestBody RequestUpdateUserEmailDTO dto,
            Authentication auth
    ) {
        emailVerificationService.updateEmail(dto, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user's password",
            description = "Updates the user's password by new one.")
    @ApiResponse(
            responseCode = "200",
            description = "Password updated successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid password format",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @PutMapping("/me/update-password")
    public ResponseEntity<Void> updatePassword(
            @RequestBody @Valid RequestUpdateUserPasswordDTO dto,
            Authentication auth
    ) {
        passwordService.updatePassword(dto, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmailUpdate(token);
        return ResponseEntity.ok().build();
    }
}
