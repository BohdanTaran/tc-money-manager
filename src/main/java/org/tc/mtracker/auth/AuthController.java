package org.tc.mtracker.auth;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.dto.AuthRequestDTO;
import org.tc.mtracker.auth.dto.AuthResponseDTO;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.tc.mtracker.image.ValidImage;
import org.tc.mtracker.security.JwtResponseDTO;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and email verification endpoints")
@Validated
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign up a new user",
            description = "Creates a new user and sends email verification link. Account not activated until verified")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseDTO.class))
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
    @PostMapping(value = "/sign-up", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthResponseDTO> signUp(
            @Parameter(
                    name = "User dto",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthRequestDTO.class))
            )
            @Valid
            @RequestPart(name = "dto") AuthRequestDTO authRequestDTO,

            @Parameter(
                    name = "Avatar",
                    required = false,
                    content = {
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary"))
                    }
            )
            @ValidImage
            @RequestPart(name = "avatar", required = false) MultipartFile avatar

    ) {
        AuthResponseDTO authResponseDTO = authService.signUp(authRequestDTO, avatar);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponseDTO);
    }

    @Operation(
            summary = "Login user",
            description = "Logins user and returns an access token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User logged successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponseDTO.class))
                    }
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Fields were filled incorrectly",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access Denied",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            ),
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login details",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequestDto.class))
            )
            @Valid @RequestBody LoginRequestDto loginRequestDto
    ) {
        JwtResponseDTO jwt = authService.login(loginRequestDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(jwt);
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
