package com.paybridge.providers.nicepay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicePayCryptoSupportTest {

    private final NicePayCryptoSupport crypto = new NicePayCryptoSupport();

    @Test
    void generatesKnownCancelSignDataFromOfficialExample() {
        String signData = crypto.generateCancelSignData(
                "nictest04m",
                "1004",
                "20191219133357",
                "b+zhZ4yOZ7FsH8pm5lhDfHZEb79tIwnjsdA0FBXh86yLc6BJeFVrZFXhAoJ3gEWgrWwN+lJMV0W4hvDdbe4Sjw=="
        );

        assertThat(signData).isEqualTo("3367c62414f8eb823819cffe4ed4e3900cb26fdf4787fa9512b3b9d8b7d8ed84");
    }

    @Test
    void generatesKnownCancelResponseSignatureFromOfficialExample() {
        String signature = crypto.expectedCancelResponseSignature(
                "nictest04m01012102030929551234",
                "nictest04m",
                "1004",
                "b+zhZ4yOZ7FsH8pm5lhDfHZEb79tIwnjsdA0FBXh86yLc6BJeFVrZFXhAoJ3gEWgrWwN+lJMV0W4hvDdbe4Sjw=="
        );

        assertThat(signature).isEqualTo("13a151050ec0327cdabf61594da65da8ba10fb45d3831931a8ee6703dce1064a");
    }

    @Test
    void buildsEncDataPlainTextWithOptionalFieldsWhenPresent() {
        NicePayKeyInCardDetails card = new NicePayKeyInCardDetails(
                "1234567890123456",
                "2512",
                "800101",
                "12"
        );

        assertThat(crypto.buildEncDataPlainText(card))
                .isEqualTo("CardNo=1234567890123456&CardExpire=2512&BuyerAuthNum=800101&CardPwd=12");
    }
}
