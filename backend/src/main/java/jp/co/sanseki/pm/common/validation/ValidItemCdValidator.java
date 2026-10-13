package jp.co.sanseki.pm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jp.co.sanseki.pm.common.util.CheckDigit;

public class ValidItemCdValidator implements ConstraintValidator<ValidItemCd, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || CheckDigit.isValid(value);
    }
}
