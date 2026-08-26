package com.pmrgsolution.shared.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<PhoneConstraint, String> {
    private static final String PHONE_PATTERN = "^\\+?[0-9]{7,15}$";
    private static final Pattern PATTERN = Pattern.compile(PHONE_PATTERN);

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null) {
            return true;
        }
        return PATTERN.matcher(phone).matches();
    }
}
