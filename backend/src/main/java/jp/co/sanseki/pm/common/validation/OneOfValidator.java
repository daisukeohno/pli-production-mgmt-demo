package jp.co.sanseki.pm.common.validation;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OneOfValidator implements ConstraintValidator<OneOf, String> {

    private Set<String> allowed;

    @Override
    public void initialize(OneOf annotation) {
        allowed = Set.of(annotation.value());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isEmpty() || allowed.contains(value);
    }
}
