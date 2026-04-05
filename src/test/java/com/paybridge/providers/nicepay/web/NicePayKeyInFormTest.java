package com.paybridge.providers.nicepay.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class NicePayKeyInFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void clearsAllSensitiveFields() {
        NicePayKeyInForm form = NicePayKeyInForm.defaultForm();
        form.setCardNumber("4111111111111111");
        form.setCardExpireYyMm("2512");
        form.setBuyerAuthNumber("800101");
        form.setCardPasswordTwoDigits("12");

        form.clearSensitiveFields();

        assertThat(form.getCardNumber()).isNull();
        assertThat(form.getCardExpireYyMm()).isNull();
        assertThat(form.getBuyerAuthNumber()).isNull();
        assertThat(form.getCardPasswordTwoDigits()).isNull();
    }

    @Test
    void rejectsValuesThatExceedEucKrByteLimit() {
        NicePayKeyInForm form = NicePayKeyInForm.defaultForm();
        form.setGoodsName("가".repeat(21));
        form.setCardNumber("4111111111111111");
        form.setCardExpireYyMm("2512");
        form.setBuyerAuthNumber("800101");
        form.setCardPasswordTwoDigits("12");

        assertThat(validator.validate(form))
                .anyMatch(violation -> "goodsName".equals(violation.getPropertyPath().toString()))
                .anyMatch(violation -> violation.getMessage().contains("40 byte limit"));
    }
}
