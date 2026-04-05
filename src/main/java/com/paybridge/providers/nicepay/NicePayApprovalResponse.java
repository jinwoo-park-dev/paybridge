package com.paybridge.providers.nicepay;

import java.util.Map;

public record NicePayApprovalResponse(
        String resultCode,
        String resultMessage,
        String tid,
        String moid,
        String amount,
        String authCode,
        String authDate,
        String cardNumber,
        String cardName,
        String cardQuota,
        String cardType,
        String partialCancelCapability,
        String cardInterest,
        String mallReserved
) {

    public static NicePayApprovalResponse from(Map<String, String> fields) {
        return new NicePayApprovalResponse(
                fields.getOrDefault("ResultCode", ""),
                fields.getOrDefault("ResultMsg", ""),
                fields.getOrDefault("TID", ""),
                fields.getOrDefault("Moid", ""),
                fields.getOrDefault("Amt", ""),
                fields.getOrDefault("AuthCode", ""),
                fields.getOrDefault("AuthDate", ""),
                fields.getOrDefault("CardNo", ""),
                fields.getOrDefault("CardName", ""),
                fields.getOrDefault("CardQuota", ""),
                fields.getOrDefault("CardCl", ""),
                fields.getOrDefault("CcPartCl", ""),
                fields.getOrDefault("CardInterest", ""),
                fields.getOrDefault("MallReserved", "")
        );
    }

    public boolean isSuccess() {
        return "3001".equals(resultCode);
    }

    public boolean partialCancelSupported() {
        return "1".equals(partialCancelCapability);
    }

    public long amountMinor() {
        return Long.parseLong(amount);
    }
}
