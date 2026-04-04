package com.paybridge.payment.application;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeDisplayFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private TimeDisplayFormatter() {
    }

    public static String format(Instant instant) {
        if (instant == null) {
            return "-";
        }
        return FORMATTER.format(instant);
    }
}
