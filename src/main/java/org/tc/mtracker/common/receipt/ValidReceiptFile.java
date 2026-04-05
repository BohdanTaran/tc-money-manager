package org.tc.mtracker.common.receipt;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReceiptValidator.class)
public @interface ValidReceiptFile {
    String message() default "Allowed receipt formats: jpg, jpeg, png, webp, pdf.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
