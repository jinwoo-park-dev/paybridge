package com.paybridge.providers.nicepay;

import java.util.Map;

public record NicePayCancelResponse(
        String resultCode,
        String resultMessage,
        String errorCode,
        String errorMessage,
        String cancelAmount,
        String merchantId,
        String moid,
        String signature,
        String payMethod,
        String tid,
        String cancelDate,
        String cancelTime,
        String cancelNumber,
        String remainAmount,
        String mallReserved
) {

    public static NicePayCancelResponse from(Map<String, String> fields) {
        return new NicePayCancelResponse(
                fields.getOrDefault("ResultCode", ""),
                fields.getOrDefault("ResultMsg", ""),
                fields.getOrDefault("ErrorCD", ""),
                fields.getOrDefault("ErrorMsg", ""),
                fields.getOrDefault("CancelAmt", ""),
                fields.getOrDefault("MID", ""),
                fields.getOrDefault("Moid", ""),
                fields.getOrDefault("Signature", ""),
                fields.getOrDefault("PayMethod", ""),
                fields.getOrDefault("TID", ""),
                fields.getOrDefault("CancelDate", ""),
                fields.getOrDefault("CancelTime", ""),
                fields.getOrDefault("CancelNum", ""),
                fields.getOrDefault("RemainAmt", ""),
                fields.getOrDefault("MallReserved", "")
        );
    }

    public boolean isSuccess() {
        return "2001".equals(resultCode);
    }

    public long cancelAmountMinor() {
        return Long.parseLong(cancelAmount);
    }

    public long remainingAmountMinor() {
        return Long.parseLong(remainAmount);
    }
}
