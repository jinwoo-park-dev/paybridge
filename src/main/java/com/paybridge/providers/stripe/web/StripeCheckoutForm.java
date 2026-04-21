package com.paybridge.providers.stripe.web;

import com.paybridge.providers.stripe.StripeCreatePaymentIntentCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class StripeCheckoutForm {

    private static final DateTimeFormatter DEMO_ORDER_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC);

    @NotBlank(message = "Order ID is required.")
    private String orderId;

    @Positive(message = "Amount (minor units) must be positive.")
    private long amountMinor;

    @NotBlank(message = "Currency is required.")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code.")
    private String currency;

    @NotBlank(message = "Description is required.")
    private String description;

    @Email(message = "Receipt email must be a valid email address.")
    private String customerEmail;

    public static StripeCheckoutForm defaultForm() {
        return seeded(
                null,
                1999L,
                "USD",
                "Monthly plan renewal",
                "buyer@example.com"
        );
    }

    public static StripeCheckoutForm seeded(
            String orderId,
            Long amountMinor,
            String currency,
            String description,
            String customerEmail
    ) {
        StripeCheckoutForm form = new StripeCheckoutForm();
        form.setOrderId(blankToDefault(orderId, newDemoOrderId()));
        form.setAmountMinor(amountMinor == null || amountMinor <= 0 ? 1999L : amountMinor);
        form.setCurrency(blankToDefault(currency, "USD").toUpperCase(Locale.ROOT));
        form.setDescription(blankToDefault(description, "Monthly plan renewal"));
        form.setCustomerEmail(blankToDefault(customerEmail, "buyer@example.com"));
        return form;
    }

    public static String newDemoOrderId() {
        String timestamp = DEMO_ORDER_TIMESTAMP.format(Instant.now());
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "ORD-STR-" + timestamp + "-" + suffix;
    }

    public StripeCreatePaymentIntentCommand toCommand() {
        return new StripeCreatePaymentIntentCommand(orderId, amountMinor, currency, description, customerEmail);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
