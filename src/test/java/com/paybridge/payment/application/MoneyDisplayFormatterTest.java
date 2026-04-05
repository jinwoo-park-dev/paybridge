package com.paybridge.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoneyDisplayFormatterTest {

    @Test
    void formatsKrwAsWholeNumber() {
        assertThat(MoneyDisplayFormatter.formatMinor("KRW", 10000L)).isEqualTo("KRW 10,000");
    }

    @Test
    void formatsUsdAsDecimal() {
        assertThat(MoneyDisplayFormatter.formatMinor("USD", 12050L)).isEqualTo("USD 120.50");
    }
}
