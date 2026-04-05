package com.paybridge.providers.nicepay;

import java.util.LinkedHashMap;
import java.util.Map;

public record NicePayApprovalRequest(
        String tid,
        String merchantId,
        String ediDate,
        String moid,
        String amount,
        String goodsName,
        String encData,
        String signData,
        String cardInterest,
        String cardQuota,
        String buyerName,
        String buyerEmail,
        String buyerTel,
        String charSet,
        String ediType
) {

    public Map<String, String> toFormParameters() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("TID", tid);
        form.put("MID", merchantId);
        form.put("EdiDate", ediDate);
        form.put("Moid", moid);
        form.put("Amt", amount);
        form.put("GoodsName", goodsName);
        form.put("EncData", encData);
        form.put("SignData", signData);
        form.put("CardInterest", cardInterest);
        form.put("CardQuota", cardQuota);
        form.put("BuyerName", buyerName);
        form.put("BuyerEmail", buyerEmail);
        form.put("BuyerTel", buyerTel);
        form.put("CharSet", charSet);
        form.put("EdiType", ediType);
        return form;
    }
}
