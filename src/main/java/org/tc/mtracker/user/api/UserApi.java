package org.tc.mtracker.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.image.ValidImage;
import org.tc.mtracker.user.dto.RequestUpdateUserProfileDTO;
import org.tc.mtracker.user.dto.ResponseUserDTO;

@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User management endpoints")
public interface UserApi {

    @Operation(
            summary = "Update user's profile",
            description = "Updates the current user's profile data and optional avatar."
    )
    @ApiResponse(
            responseCode = "200",
            description = "User profile updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ResponseUserDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ResponseUserDTO> updateMe(
            @Parameter(
                    name = "User profile update DTO",
                    required = false,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RequestUpdateUserProfileDTO.class)
                    )
            )
            @Valid @RequestPart(name = "dto", required = false) RequestUpdateUserProfileDTO dto,
            @Parameter(
                    name = "avatar",
                    required = false,
                    description = "Allowed formats: jpg, jpeg, png, gif, webp.",
                    content = {
                            @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = MediaType.IMAGE_GIF_VALUE, schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary"))
                    }
            )
            @ValidImage @RequestPart(name = "avatar", required = false) MultipartFile avatar,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Get current user profile",
            description = "Returns the authenticated user's profile."
    )
    @ApiResponse(
            responseCode = "200",
            description = "User profile returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ResponseUserDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "User not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "User with username 'Alex Noob' not found"))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "Full authentication is required to access this resource"))
    )
    @ApiResponse(
            responseCode = "405",
            description = "Method not allowed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "The method is not supported: POST"))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "Internal error: NullPointerException"))
    )
    @GetMapping("/me")
    ResponseEntity<ResponseUserDTO> getUserProfile(@Parameter(hidden = true) Authentication auth);
}
