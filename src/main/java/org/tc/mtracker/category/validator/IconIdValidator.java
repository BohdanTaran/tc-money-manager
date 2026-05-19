package org.tc.mtracker.category.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.tc.mtracker.category.enums.IconIds;

public class IconIdValidator implements ConstraintValidator<IconId, String> {

    @Override
    public boolean isValid(String icon, ConstraintValidatorContext context) {
        if (icon == null) {
            return true;
        }
        return IconIds.iconSet.contains(icon);
    }
}
