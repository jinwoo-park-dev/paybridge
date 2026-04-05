package com.paybridge.providers.nicepay;

import java.util.LinkedHashMap;
import java.util.Map;

public record NicePayCancelRequest(
        String tid,
        String merchantId,
        String moid,
        String cancelAmount,
        String cancelMessage,
        String partialCancelCode,
        String ediDate,
        String signData,
        String charSet,
        String ediType
) {

    public Map<String, String> toFormParameters() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("TID", tid);
        form.put("MID", merchantId);
        form.put("Moid", moid);
        form.put("CancelAmt", cancelAmount);
        form.put("CancelMsg", cancelMessage);
        form.put("PartialCancelCode", partialCancelCode);
        form.put("EdiDate", ediDate);
        form.put("SignData", signData);
        form.put("CharSet", charSet);
        form.put("EdiType", ediType);
        return form;
    }
}
