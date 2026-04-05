package com.paybridge.providers.nicepay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicePayTidGeneratorTest {

    private final NicePayTidGenerator generator = new NicePayTidGenerator();

    @Test
    void generatesCreditCardTidWithNicePayRule() {
        String tid = generator.nextCreditCardTid("nictest04m");

        assertThat(tid)
                .hasSize(30)
                .startsWith("nictest04m0101")
                .matches("nictest04m0101\\d{16}");
    }

    @Test
    void generatesEdiDateInRequiredFormat() {
        String ediDate = generator.nextEdiDate();
        assertThat(ediDate).hasSize(14).matches("\\d{14}");
    }
}
