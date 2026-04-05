package com.paybridge.support.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveValueMaskerTest {

    @Test
    void masksSecretsAndIdentifiers() {
        assertThat(SensitiveValueMasker.maskSecret("sk_test_1234567890")).isEqualTo("sk_t…7890");
        assertThat(SensitiveValueMasker.maskProviderIdentifier("pi_1234567890abcdef")).isEqualTo("pi_1…cdef");
        assertThat(SensitiveValueMasker.maskEmail("tester@example.com")).isEqualTo("t***@example.com");
    }

    @Test
    void masksSignatureHeader() {
        assertThat(SensitiveValueMasker.maskSignatureHeader("t=12345,v1=abcdefg")).isEqualTo("t=12345,v1=********");
    }
}
