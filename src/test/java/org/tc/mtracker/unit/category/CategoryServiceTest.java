package org.tc.mtracker.unit.category;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.CategoryMapper;
import org.tc.mtracker.category.CategoryRepository;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.category.dto.CategoryResponseDTO;
import org.tc.mtracker.category.dto.CreateCategoryDTO;
import org.tc.mtracker.category.dto.UpdateCategoryDTO;
import org.tc.mtracker.category.enums.CategoryIcon;
import org.tc.mtracker.category.enums.CategoryScope;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.CategoryAlreadyExistsException;
import org.tc.mtracker.utils.exceptions.CategoryIsImmutableException;
import org.tc.mtracker.utils.exceptions.CategoryReplacementRequiredException;
import org.tc.mtracker.utils.exceptions.InvalidCategoryReplacementException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldNormalizeBlankFiltersAndLoadAccessibleCategories() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        List<Category> categories = List.of(
                EntityTestFactory.category(
                        1L,
                        null,
                        "Random Income",
                        TransactionType.INCOME,
                        CategoryStatus.ACTIVE,
                        CategoryScope.USER)
        );
        List<CategoryResponseDTO> response = List.of(
                new CategoryResponseDTO(1L,
                        "Random Income",
                        TransactionType.INCOME,
                        CategoryStatus.ACTIVE,
                        CategoryScope.USER,
                        "icon")
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findGlobalAndUserCategories(
                eq(user),
                isNull(),
                eq(List.of(TransactionType.values())),
                eq(CategoryStatus.ACTIVE)
        )).thenReturn(categories);
        when(categoryMapper.toListDto(categories)).thenReturn(response);

        List<CategoryResponseDTO> result = categoryService.getCategories("   ", null, false, authentication);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldCreateCategoryWhenNameAndTypeAreUnique() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        CreateCategoryDTO dto = new CreateCategoryDTO(
                "Education",
                TransactionType.EXPENSE,
                CategoryIcon.DATABASE);
        Category saved = EntityTestFactory.category(
                3L,
                user,
                "Education",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);
        CategoryResponseDTO response = new CategoryResponseDTO(
                3L,
                "Education",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.GLOBAL,
                "TREND_UP");

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findAllByNameAndUser(dto.name(), user)).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(response);

        CategoryResponseDTO result = categoryService.createCategory(dto, authentication);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        assertThat(result).isEqualTo(response);
        assertThat(captor.getValue().getName()).isEqualTo("Education");
        assertThat(captor.getValue().getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void shouldRejectDuplicateCategoryDuringCreate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        CreateCategoryDTO dto = new CreateCategoryDTO(
                "Random Income",
                TransactionType.INCOME,
                CategoryIcon.COINS);
        Category existing = EntityTestFactory.category(
                1L,
                user,
                "Random Income",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findAllByNameAndUser(dto.name(), user)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> categoryService.createCategory(dto, authentication))
                .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldUpdateOwnedCategory() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category category = EntityTestFactory.category(
                3L,
                user,
                "Side Project",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);
        UpdateCategoryDTO dto = new UpdateCategoryDTO("Own Project", CategoryIcon.BRIEFCASE);
        CategoryResponseDTO response = new CategoryResponseDTO(
                3L,
                "Own Project",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.USER,
                "briefcase");

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(category));
        when(categoryRepository.findAllByNameAndUser(dto.name(), user)).thenReturn(List.of());
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(3L, dto, 1L);

        assertThat(result).isEqualTo(response);
        assertThat(category.getName()).isEqualTo("Own Project");
        assertThat(category.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
    }

    @Test
    void shouldArchiveCategoryOnlyWhenItIsActive() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category activeCategory = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(activeCategory));

        categoryService.archiveCategory(3L, authentication);

        assertThat(activeCategory.getStatus()).isEqualTo(CategoryStatus.ARCHIVED);
        verify(categoryRepository).save(activeCategory);
    }

    @Test
    void shouldSkipSavingAlreadyArchivedCategory() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category archivedCategory = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ARCHIVED,
                CategoryScope.USER
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(archivedCategory));

        categoryService.archiveCategory(3L, authentication);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldUnarchiveCategoryOnlyWhenItIsArchived() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category archivedCategory = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ARCHIVED,
                CategoryScope.USER);

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(archivedCategory));

        categoryService.unarchiveCategory(3L, authentication);

        assertThat(archivedCategory.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        verify(categoryRepository).save(archivedCategory);
    }

    @Test
    void shouldDeleteUnusedCategoryWithoutReplacement() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category category = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(category));
        when(transactionRepository.countByUserAndCategory(user.getId(), category)).thenReturn(0L);
        when(recurringTransactionRepository.countByUserAndCategory(user.getId(), category)).thenReturn(0L);

        categoryService.deleteCategory(3L, null, 1L);

        verify(transactionRepository, never()).reassignCategory(any(), any(), any());
        verify(recurringTransactionRepository, never()).reassignCategory(any(), any(), any());
        verify(categoryRepository).delete(category);
    }

    @Test
    void shouldRequireReplacementWhenDeletingUsedCategory() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category category = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(category));
        when(transactionRepository.countByUserAndCategory(user.getId(), category)).thenReturn(2L);
        when(recurringTransactionRepository.countByUserAndCategory(user.getId(), category)).thenReturn(0L);

        assertThatThrownBy(() -> categoryService.deleteCategory(3L, null, 1L))
                .isInstanceOf(CategoryReplacementRequiredException.class)
                .hasMessage("Replacement category is required when category is already used.");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void shouldReassignTransactionsAndRecurringTransactionsBeforeDelete() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category sourceCategory = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);
        Category replacementCategory = EntityTestFactory.category(
                5L,
                null,
                "Housing",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(sourceCategory));
        when(categoryRepository.findAccessibleById(5L, user)).thenReturn(Optional.of(replacementCategory));
        when(transactionRepository.countByUserAndCategory(user.getId(), sourceCategory)).thenReturn(2L);
        when(recurringTransactionRepository.countByUserAndCategory(user.getId(), sourceCategory)).thenReturn(1L);

        categoryService.deleteCategory(3L, 5L, 1L);

        verify(transactionRepository).reassignCategory(user.getId(), sourceCategory, replacementCategory);
        verify(recurringTransactionRepository).reassignCategory(user.getId(), sourceCategory, replacementCategory);
        verify(categoryRepository).delete(sourceCategory);
    }

    @Test
    void shouldRejectArchivedReplacementCategoryDuringDelete() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category sourceCategory = EntityTestFactory.category(
                3L,
                user,
                "Rent",
                TransactionType.EXPENSE,
                CategoryStatus.ACTIVE,
                CategoryScope.USER);
        Category replacementCategory = EntityTestFactory.category(
                5L,
                null,
                "Housing",
                TransactionType.EXPENSE,
                CategoryStatus.ARCHIVED,
                CategoryScope.USER);

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(3L, user)).thenReturn(Optional.of(sourceCategory));
        when(transactionRepository.countByUserAndCategory(user.getId(), sourceCategory)).thenReturn(1L);
        when(recurringTransactionRepository.countByUserAndCategory(user.getId(), sourceCategory)).thenReturn(0L);
        when(categoryRepository.findAccessibleById(5L, user)).thenReturn(Optional.of(replacementCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(3L, 5L, 1L))
                .isInstanceOf(InvalidCategoryReplacementException.class)
                .hasMessage("Replacement category must be active.");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingGlobalCategory() {
        // given
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category globalCategory = EntityTestFactory.category(
                1L,
                null,
                "SALARY",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.GLOBAL
        );
        UpdateCategoryDTO dto = new UpdateCategoryDTO("Updated Salary", CategoryIcon.DOLLAR);

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(1L, user)).thenReturn(Optional.of(globalCategory));

        // when & then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, dto, 1L))
                .isInstanceOf(CategoryIsImmutableException.class)
                .hasMessageContaining("Category SALARY has GLOBAL scope and can not be changed");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldThrowExceptionWhenArchivingGlobalCategory() {
        // given
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category globalCategory = EntityTestFactory.category(
                1L,
                null,
                "SALARY",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.GLOBAL
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findOwnedById(1L, user)).thenReturn(Optional.of(globalCategory));

        // when & then
        assertThatThrownBy(() -> categoryService.archiveCategory(1L, authentication))
                .isInstanceOf(CategoryIsImmutableException.class)
                .hasMessageContaining("Category SALARY has GLOBAL scope and can not be changed");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldThrowExceptionWhenDeletingGlobalCategory() {
        // given
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category globalCategory = EntityTestFactory.category(
                1L,
                null,
                "SALARY",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.GLOBAL
        );

        when(userService.getUserById(1L)).thenReturn(user);
        when(categoryRepository.findOwnedById(1L, user)).thenReturn(Optional.of(globalCategory));

        // when & then
        assertThatThrownBy(() -> categoryService.deleteCategory(
                1L,
                null,
                1L))
                .isInstanceOf(CategoryIsImmutableException.class)
                .hasMessageContaining("Category SALARY has GLOBAL scope and can not be changed");

        verify(categoryRepository, never()).delete(any(Category.class));
        verify(transactionRepository, never()).reassignCategory(any(), any(), any());
        verify(recurringTransactionRepository, never()).reassignCategory(any(), any(), any());
    }

    @Test
    void shouldReturnGlobalAndUserCategories() {
        // given
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Category globalCategory = EntityTestFactory.category(
                1L,
                null,
                "SALARY",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.GLOBAL
        );
        Category userCategory = EntityTestFactory.category(
                2L,
                user,
                "Freelance",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE,
                CategoryScope.USER
        );
        List<Category> categories = List.of(globalCategory, userCategory);
        List<CategoryResponseDTO> response = List.of(
                new CategoryResponseDTO(
                        1L,
                        "SALARY",
                        TransactionType.INCOME,
                        CategoryStatus.ACTIVE,
                        CategoryScope.GLOBAL,
                        "icon"),
                new CategoryResponseDTO(
                        2L,
                        "Freelance",
                        TransactionType.INCOME,
                        CategoryStatus.ACTIVE,
                        CategoryScope.USER,
                        "icon")
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(categoryRepository.findGlobalAndUserCategories(
                eq(user),
                isNull(),
                eq(List.of(TransactionType.values())),
                eq(CategoryStatus.ACTIVE)
        )).thenReturn(categories);
        when(categoryMapper.toListDto(categories)).thenReturn(response);

        // when
        List<CategoryResponseDTO> result =
                categoryService.getCategories(null, null, false, authentication);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("scope").containsExactly(CategoryScope.GLOBAL, CategoryScope.USER);
    }
}
