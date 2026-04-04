package com.paybridge.payment.application;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyDisplayFormatter {

    private static final DecimalFormatSymbols US_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private MoneyDisplayFormatter() {
    }

    public static String formatMinor(String currency, long minorAmount) {
        if (currency == null) {
            return String.valueOf(minorAmount);
        }

        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        return switch (normalizedCurrency) {
            case "KRW", "JPY" -> normalizedCurrency + " " + integerFormat().format(minorAmount);
            default -> normalizedCurrency + " "
                    + decimalFormat().format(BigDecimal.valueOf(minorAmount, 2));
        };
    }

    private static DecimalFormat integerFormat() {
        return new DecimalFormat("#,##0", US_SYMBOLS);
    }

    private static DecimalFormat decimalFormat() {
        return new DecimalFormat("#,##0.00", US_SYMBOLS);
    }
}
