package com.paybridge.support.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = EucKrByteLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface EucKrByteLength {

    String message() default "Value exceeds the configured EUC-KR byte limit.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    int max();
}
