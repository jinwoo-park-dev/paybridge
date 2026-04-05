package com.paybridge.providers.nicepay.web;

import com.paybridge.providers.nicepay.NicePayKeyInApprovalCommand;
import com.paybridge.providers.nicepay.NicePayKeyInCardDetails;
import com.paybridge.support.validation.EucKrByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NicePayKeyInForm {

    @NotBlank(message = "Order ID is required.")
    @Size(max = 64, message = "Order ID must be 64 characters or fewer.")
    private String orderId;

    @Min(value = 1, message = "Amount must be positive.")
    @Max(value = 999_999_999L, message = "Amount exceeds the supported NicePay key-in range.")
    private long amountMinor;

    @NotBlank(message = "Goods / order description is required.")
    @EucKrByteLength(max = 40, message = "Goods / order description must fit NicePay's 40 byte limit.")
    private String goodsName;

    @NotBlank(message = "Buyer name is required.")
    @EucKrByteLength(max = 30, message = "Buyer name must fit NicePay's 30 byte limit.")
    private String buyerName;

    @Email(message = "Buyer email must be a valid email address.")
    @EucKrByteLength(max = 60, message = "Buyer email must fit NicePay's 60 byte limit.")
    private String buyerEmail;

    @EucKrByteLength(max = 20, message = "Buyer phone must fit NicePay's 20 byte limit.")
    private String buyerTel;

    @NotBlank(message = "Card number is required.")
    @Pattern(regexp = "\\d{12,19}", message = "Card number must contain digits only.")
    private String cardNumber;

    @NotBlank(message = "Card expiry is required.")
    @Pattern(regexp = "\\d{4}", message = "Card expiry must be YYMM.")
    private String cardExpireYyMm;

    @Pattern(regexp = "\\d{0,13}", message = "Buyer auth number must contain digits only.")
    private String buyerAuthNumber;

    @Pattern(regexp = "(^$|\\d{2})", message = "Card password prefix must be 2 digits when provided.")
    private String cardPasswordTwoDigits;

    @Pattern(regexp = "[01]", message = "Interest option must be 0 or 1.")
    private String cardInterest = "0";

    @Pattern(regexp = "\\d{2}", message = "Installment months must be two digits.")
    private String cardQuota = "00";

    public static NicePayKeyInForm defaultForm() {
        return seeded(
                "ORD-NP-2026-1001",
                10_000L,
                "Monthly plan renewal",
                "Alex Kim",
                "buyer@example.com",
                "01012345678"
        );
    }

    public static NicePayKeyInForm seeded(
            String orderId,
            Long amountMinor,
            String goodsName,
            String buyerName,
            String buyerEmail,
            String buyerTel
    ) {
        NicePayKeyInForm form = new NicePayKeyInForm();
        form.setOrderId(blankToDefault(orderId, "ORD-NP-2026-1001"));
        form.setAmountMinor(amountMinor == null || amountMinor <= 0 ? 10_000L : amountMinor);
        form.setGoodsName(blankToDefault(goodsName, "Monthly plan renewal"));
        form.setBuyerName(blankToDefault(buyerName, "Alex Kim"));
        form.setBuyerEmail(blankToDefault(buyerEmail, "buyer@example.com"));
        form.setBuyerTel(blankToDefault(buyerTel, "01012345678"));
        form.setCardInterest("0");
        form.setCardQuota("00");
        return form;
    }

    public NicePayKeyInApprovalCommand toCommand() {
        return new NicePayKeyInApprovalCommand(
                orderId,
                amountMinor,
                goodsName,
                buyerName,
                blankToNull(buyerEmail),
                blankToNull(buyerTel),
                new NicePayKeyInCardDetails(cardNumber, cardExpireYyMm, blankToNull(buyerAuthNumber), blankToNull(cardPasswordTwoDigits)),
                cardInterest,
                cardQuota
        );
    }

    public void clearSensitiveFields() {
        this.cardNumber = null;
        this.cardExpireYyMm = null;
        this.buyerAuthNumber = null;
        this.cardPasswordTwoDigits = null;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public String getBuyerTel() { return buyerTel; }
    public void setBuyerTel(String buyerTel) { this.buyerTel = buyerTel; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardExpireYyMm() { return cardExpireYyMm; }
    public void setCardExpireYyMm(String cardExpireYyMm) { this.cardExpireYyMm = cardExpireYyMm; }
    public String getBuyerAuthNumber() { return buyerAuthNumber; }
    public void setBuyerAuthNumber(String buyerAuthNumber) { this.buyerAuthNumber = buyerAuthNumber; }
    public String getCardPasswordTwoDigits() { return cardPasswordTwoDigits; }
    public void setCardPasswordTwoDigits(String cardPasswordTwoDigits) { this.cardPasswordTwoDigits = cardPasswordTwoDigits; }
    public String getCardInterest() { return cardInterest; }
    public void setCardInterest(String cardInterest) { this.cardInterest = cardInterest; }
    public String getCardQuota() { return cardQuota; }
    public void setCardQuota(String cardQuota) { this.cardQuota = cardQuota; }
}
