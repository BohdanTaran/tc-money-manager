package org.tc.mtracker.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.user.dto.RequestUpdateUserEmailDTO;
import org.tc.mtracker.user.dto.ResponseUserProfileDTO;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
import org.tc.mtracker.utils.image.ValidImage;

@Tag(name = "User Management", description = "User management endpoints")
public interface UserOperations {

    @Operation(summary = "Update user's profile",
            description = "Updates the user's data by new one.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's data updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseUserProfileDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ResponseUserProfileDTO> updateMe(
            @Parameter(
                    name = "User profile update DTO",
                    required = false,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
            @Valid
            @RequestPart(name = "dto", required = false) UpdateUserProfileDTO dto,
            @Parameter(
                    name = "avatar",
                    required = false,
                    content = {
                            @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary")),
                    }
            )
            @ValidImage
            @RequestParam(name = "avatar", required = false) MultipartFile avatar,
            @Parameter(hidden = true) Authentication auth
    );

    @PostMapping(value = "/me/update-email")
    ResponseEntity<ResponseUserProfileDTO> updateEmail(
            @RequestBody RequestUpdateUserEmailDTO dto,
            @Parameter(hidden = true) Authentication auth
    );

    @GetMapping(value = "/verify-email")
    ResponseEntity<Void> verifyEmail(@RequestParam String token);
}
