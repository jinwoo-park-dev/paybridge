package com.paybridge.support.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.Charset;

public class EucKrByteLengthValidator implements ConstraintValidator<EucKrByteLength, String> {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private int max;

    @Override
    public void initialize(EucKrByteLength constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.getBytes(EUC_KR).length <= max;
    }
}
