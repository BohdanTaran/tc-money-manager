package org.tc.mtracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tc.mtracker.dto.JwtResponseDTO;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.dto.UserSignUpResponseDTO;
import org.tc.mtracker.service.AuthService;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and email verification endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign up a new user",
            description = "Creates a new user and sends email verification link. Account not activated until verified")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserSignUpResponseDTO.class))
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User with this email already exists",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            )
    })
    @PostMapping("/sign-up")
    public ResponseEntity<UserSignUpResponseDTO> signUp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User sign up details",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserSignUpRequestDTO.class))
            )
            @Valid @RequestBody UserSignUpRequestDTO userSignUpRequestDTO) {
        UserSignUpResponseDTO userSignUpResponseDTO = authService.signUp(userSignUpRequestDTO);
        return ResponseEntity.ok().body(userSignUpResponseDTO);
    }

    @Operation(
            summary = "Verify email by verification token",
            description = "Activates user account using email verification token and returns access JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User activated, access token issued",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid token (wrong purpose/format)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already activated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT parsing/validation error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/verify")
    public ResponseEntity<JwtResponseDTO> verifyToken(
            @Parameter(
                    name = "token",
                    in = ParameterIn.QUERY,
                    description = "Email verification token JWT with purpose=email_verification",
                    required = true,
                    schema = @Schema(type = "string")
            )
            @RequestParam String token) {
        JwtResponseDTO jwt = authService.verifyToken(token);
        return ResponseEntity.ok().body(jwt);
    }
}
