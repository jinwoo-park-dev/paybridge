package com.paybridge.providers.nicepay;

public record NicePayKeyInApprovalCommand(
        String orderId,
        long amountMinor,
        String goodsName,
        String buyerName,
        String buyerEmail,
        String buyerTel,
        NicePayKeyInCardDetails cardDetails,
        String cardInterest,
        String cardQuota
) {
}
