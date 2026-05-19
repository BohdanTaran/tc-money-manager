package org.tc.mtracker.category.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = IconIdValidator.class)
public @interface IconId {
    String message() default "State must be one of {validStates}!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
