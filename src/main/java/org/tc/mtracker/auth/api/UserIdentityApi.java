package org.tc.mtracker.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.tc.mtracker.auth.dto.UpdateEmailRequestDto;
import org.tc.mtracker.auth.dto.UpdatePasswordRequestDto;

@RequestMapping("/api/v1/users")
@Tag(name = "User Identity", description = "Email and password management endpoints")
public interface UserIdentityApi {

    @Operation(
            summary = "Request email change",
            description = "Sends an email verification flow for updating the current user's email."
    )
    @ApiResponse(responseCode = "200", description = "Email update verification initiated")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping("/me/update-email")
    ResponseEntity<Void> updateEmail(
            @Valid @RequestBody UpdateEmailRequestDto dto,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Update user's password",
            description = "Updates the current user's password."
    )
    @ApiResponse(responseCode = "200", description = "Password updated successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid password format",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PutMapping("/me/update-password")
    ResponseEntity<Void> updatePassword(
            @Valid @RequestBody UpdatePasswordRequestDto dto,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Confirm email change",
            description = "Confirms the new email address using a verification token."
    )
    @ApiResponse(responseCode = "200", description = "Email updated successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid verification token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(@RequestParam String token);
}
