package org.tc.mtracker.category.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.category.dto.CategoryResponseDTO;
import org.tc.mtracker.category.dto.CreateCategoryDTO;
import org.tc.mtracker.category.dto.UpdateCategoryDTO;
import org.tc.mtracker.common.enums.TransactionType;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    @Override
    public ResponseEntity<List<CategoryResponseDTO>> getCategories(
            String name,
            List<TransactionType> type,
            boolean archived,
            Authentication auth
    ) {
        return ResponseEntity.ok((categoryService.getCategories(name, type, archived, auth)));
    }

    @Override
    public ResponseEntity<CategoryResponseDTO> getCategoryById(
            Long categoryId,
            Authentication auth
    ) {
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId, auth));
    }

    @Override
    public ResponseEntity<CategoryResponseDTO> createCategory(
            CreateCategoryDTO dto,
            Authentication auth
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.createCategory(dto, auth));
    }

    @Override
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            Long categoryId,
            UpdateCategoryDTO dto,
            Authentication auth
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, dto, auth));
    }

    @Override
    public ResponseEntity<Void> archiveCategory(
            Long categoryId,
            Authentication auth
    ) {
        categoryService.archiveCategory(categoryId, auth);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> unarchiveCategory(
            Long categoryId,
            Authentication auth
    ) {
        categoryService.unarchiveCategory(categoryId, auth);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCategory(
            Long categoryId,
            Long replacementCategoryId,
            Authentication auth
    ) {
        categoryService.deleteCategory(categoryId, replacementCategoryId, auth);
        return ResponseEntity.noContent().build();
    }
}
