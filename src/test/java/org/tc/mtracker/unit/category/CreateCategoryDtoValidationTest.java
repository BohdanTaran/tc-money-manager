package org.tc.mtracker.unit.category;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.tc.mtracker.category.dto.CreateCategoryDTO;
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
            "award, true",
            "trend_up, true",
            "dollar, true",
            "invalid-icon, false",
            "nonexistent, false",
            "random, false"
    })
    void shouldValidateIcon(String icon, boolean isValid) {
        var dto = new CreateCategoryDTO("Salary", TransactionType.INCOME, icon);

        Set<ConstraintViolation<CreateCategoryDTO>> violations = validator.validate(dto);

        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("icon") &&
                            v.getMessage().equals("Incorrect category icon id")
            );
        }
    }

    @ParameterizedTest
    @CsvSource({
            "null, Category's icon should not be null or empty",
            "'', Category's icon should not be null or empty",
            "'   ', Category's icon should not be null or empty"
    })
    void shouldRejectNullOrBlankIcon(String icon) {
        String iconValue = icon.equals("null") ? null : icon;
        var dto = new CreateCategoryDTO("Salary", TransactionType.INCOME, iconValue);

        Set<ConstraintViolation<CreateCategoryDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v ->
                        v.getPropertyPath().toString().equals("icon") &&
                                v.getMessage().equals("Category's icon should not be null or empty")
                );
    }
}