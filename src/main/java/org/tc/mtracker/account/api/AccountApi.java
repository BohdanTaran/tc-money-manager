package org.tc.mtracker.account.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tc.mtracker.account.dto.AccountResponseDTO;

@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Management", description = "Account management endpoints")
public interface AccountApi {

    @Operation(
            summary = "Get default account",
            description = "Returns the authenticated user's default account."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Default account returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AccountResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Default account not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/default")
    ResponseEntity<AccountResponseDTO> getDefaultAccount(@Parameter(hidden = true) Authentication auth);
}
