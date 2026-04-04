package com.paybridge.support.logging;

public final class SensitiveValueMasker {

    private SensitiveValueMasker() {
    }

    public static String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "(blank)";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "********";
        }
        return trimmed.substring(0, 4) + "…" + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskSignatureHeader(String value) {
        if (value == null || value.isBlank()) {
            return "(blank)";
        }
        int v1Index = value.indexOf("v1=");
        if (v1Index < 0) {
            return maskSecret(value);
        }
        String prefix = value.substring(0, Math.min(v1Index + 3, value.length()));
        return prefix + "********";
    }

    public static String maskEmail(String value) {
        if (value == null || value.isBlank()) {
            return "(blank)";
        }
        String trimmed = value.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 1) {
            return "***" + trimmed.substring(Math.max(0, atIndex));
        }
        return trimmed.substring(0, 1) + "***" + trimmed.substring(atIndex);
    }

    public static String maskProviderIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "(blank)";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return trimmed.substring(0, Math.min(3, trimmed.length())) + "***";
        }
        return trimmed.substring(0, 4) + "…" + trimmed.substring(trimmed.length() - 4);
    }
}
