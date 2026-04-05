package com.paybridge.providers.nicepay;

import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class NicePayCryptoSupport {

    public String encryptEncData(NicePayKeyInCardDetails cardDetails, String merchantKey) {
        try {
            String plainText = buildEncDataPlainText(cardDetails);
            SecretKeySpec keySpec = new SecretKeySpec(extractMerchantKeyPrefix(merchantKey).getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return toHex(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (PayBridgeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PayBridgeException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Failed to generate NicePay EncData.");
        }
    }

    public String generateApprovalSignData(String merchantId, String amount, String ediDate, String moid, String merchantKey) {
        return sha256Hex(required(merchantId, "merchantId") + required(amount, "amount") + required(ediDate, "ediDate") + required(moid, "moid") + required(merchantKey, "merchantKey"));
    }

    public String generateCancelSignData(String merchantId, String cancelAmount, String ediDate, String merchantKey) {
        return sha256Hex(required(merchantId, "merchantId") + required(cancelAmount, "cancelAmount") + required(ediDate, "ediDate") + required(merchantKey, "merchantKey"));
    }

    public String expectedCancelResponseSignature(String tid, String merchantId, String cancelAmount, String merchantKey) {
        return sha256Hex(
            required(tid, "tid")
                + required(merchantId, "merchantId")
                + normalizeNicePayAmount(cancelAmount, "cancelAmount")
                + required(merchantKey, "merchantKey")
        );
    }

    public String buildEncDataPlainText(NicePayKeyInCardDetails cardDetails) {
        if (cardDetails == null) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "NicePay card details are required.");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("CardNo=").append(required(cardDetails.cardNumber(), "cardNumber"));
        builder.append("&CardExpire=").append(required(cardDetails.cardExpireYyMm(), "cardExpireYyMm"));

        if (hasText(cardDetails.buyerAuthNumber())) {
            builder.append("&BuyerAuthNum=").append(cardDetails.buyerAuthNumber().trim());
        }
        if (hasText(cardDetails.cardPasswordTwoDigits())) {
            builder.append("&CardPwd=").append(cardDetails.cardPasswordTwoDigits().trim());
        }
        return builder.toString();
    }

    public String sha256Hex(String plainText) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return toHex(messageDigest.digest(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 must be available on the JVM.", ex);
        }
    }

    private String extractMerchantKeyPrefix(String merchantKey) {
        String normalized = required(merchantKey, "merchantKey");
        if (normalized.length() < 16) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "NicePay merchant key must be at least 16 characters.");
        }
        return normalized.substring(0, 16);
    }

    private String normalizeNicePayAmount(String value, String fieldName) {
        String normalized = required(value, fieldName);
        try {
            return String.format("%012d", Long.parseLong(normalized));
        } catch (NumberFormatException ex) {
            throw new PayBridgeException(
                HttpStatus.BAD_GATEWAY,
                ErrorCode.PROVIDER_ERROR,
                fieldName + " must be numeric for NicePay signature verification."
            );
        }
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
