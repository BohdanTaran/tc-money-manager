package org.tc.mtracker.unit.category;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.tc.mtracker.category.dto.CreateCategoryDTO;
import org.tc.mtracker.category.enums.CategoryIcon;
import org.tc.mtracker.common.enums.TransactionType;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCategoryDtoValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "TREND_UP, true",
            "AWARD, true",
            "DOLLAR, true",
            "WALLET, true",
            "INVALID, false"
    })
    void shouldValidateIcon(String iconName, boolean isValid) {
        CategoryIcon icon = isValid ? CategoryIcon.valueOf(iconName) : null;

        var dto = new CreateCategoryDTO("Salary", TransactionType.INCOME, icon);

        Set<ConstraintViolation<CreateCategoryDTO>> violations = validator.validate(dto);

        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
        }
    }

    @Test
    void shouldRejectNullOrBlankIcon() {
        var dtoNull = new CreateCategoryDTO("Salary", TransactionType.INCOME, null);
        Set<ConstraintViolation<CreateCategoryDTO>> violationsNull = validator.validate(dtoNull);

        assertThat(violationsNull)
                .isNotEmpty()
                .anyMatch(v ->
                        v.getPropertyPath().toString().equals("icon") &&
                                v.getMessage().equals("Category's icon should not be null")
                );
    }
}