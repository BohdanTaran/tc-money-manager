package org.tc.mtracker.category.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.tc.mtracker.category.dto.CategoryResponseDTO;
import org.tc.mtracker.category.dto.CreateCategoryDTO;
import org.tc.mtracker.category.dto.UpdateCategoryDTO;
import org.tc.mtracker.common.enums.TransactionType;

import java.util.List;

@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "Category management endpoints")
public interface CategoryApi {

    @Operation(
            summary = "Get available categories",
            description = "Returns global and user-owned categories filtered by name, transaction type, and archive state."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved categories",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping
    ResponseEntity<List<CategoryResponseDTO>> getCategories(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) List<TransactionType> type,
            @RequestParam(value = "archived", defaultValue = "false") boolean archived,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Get category by id",
            description = "Returns a category that is accessible to the authenticated user."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Category returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CategoryResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/{categoryId}")
    ResponseEntity<CategoryResponseDTO> getCategoryById(
            @PathVariable("categoryId") Long categoryId,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Create a new category",
            description = "Creates a custom category for the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Category created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category with this name and type already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping
    ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CreateCategoryDTO dto,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Update category",
            description = "Updates a user-owned category name, type, and icon."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Category updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CategoryResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PutMapping("/{categoryId}")
    ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody UpdateCategoryDTO dto,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Archive category",
            description = "Archives a user-owned category. Archived categories stay in existing transactions but cannot be used for new ones."
    )
    @ApiResponse(responseCode = "204", description = "Category archived successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PatchMapping("/{categoryId}/archive")
    ResponseEntity<Void> archiveCategory(
            @PathVariable("categoryId") Long categoryId,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Unarchive category",
            description = "Restores an archived user-owned category back to the active state."
    )
    @ApiResponse(responseCode = "204", description = "Category restored successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PatchMapping("/{categoryId}/unarchive")
    ResponseEntity<Void> unarchiveCategory(
            @PathVariable("categoryId") Long categoryId,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Delete category",
            description = "Deletes a user-owned category. If transactions or recurring transactions already use it, provide replacementCategoryId to move those references first."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Replacement category is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @DeleteMapping("/{categoryId}")
    ResponseEntity<Void> deleteCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(value = "replacementCategoryId", required = false) Long replacementCategoryId,
            @Parameter(hidden = true) Authentication auth
    );
}
