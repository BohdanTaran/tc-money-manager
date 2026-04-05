package org.tc.mtracker.common.image;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageValidator.class)
public @interface ValidImage {
    String message() default "Allowed image formats: jpg, jpeg, png, gif, webp.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
