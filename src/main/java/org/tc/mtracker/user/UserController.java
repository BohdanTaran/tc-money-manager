package org.tc.mtracker.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.user.dto.ResponseUserAvatarUrlDTO;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
import org.tc.mtracker.user.image.ValidImage;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management endpoints")
@Validated
public class UserController {

    private final UserService userService;

    @Operation(summary = "Update user's profile",
            description = "Updates the user's data by new one.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's data updated successfully",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "User updated successfully!")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class))
                    }
            )
    })
    @PutMapping("/me")
    public ResponseEntity<String> updateMe(
            @RequestBody @Valid UpdateUserProfileDTO dto,
            @Parameter(hidden = true) Authentication auth
    ) {
        userService.updateProfile(dto, auth);
        return ResponseEntity.ok()
                .body("User updated successfully!");
    }

    @Operation(summary = "Upload user's avatar")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Avatar uploaded successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseUserAvatarUrlDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid avatar format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
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
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseUserAvatarUrlDTO> uploadAvatar(
            @Parameter(
                    name = "Avatar",
                    required = true,
                    content = {
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary"))
                    }
            )
            @ValidImage
            @RequestParam("avatar") MultipartFile avatar,
            @Parameter(hidden = true) Authentication auth) {
        String url = userService.uploadAvatar(avatar, auth);
        return ResponseEntity.ok().body(new ResponseUserAvatarUrlDTO(url));
    }
}
