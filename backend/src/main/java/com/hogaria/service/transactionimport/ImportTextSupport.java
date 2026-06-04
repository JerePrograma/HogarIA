package com.hogaria.service.transactionimport;

import java.text.Normalizer;
import java.util.Locale;

public final class ImportTextSupport {

    public static final String DEFAULT_CURRENCY = "ARS";

    private ImportTextSupport() {
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (var value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        var clean = value
                .replace('\u00A0', ' ')
                .trim();

        clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return clean
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeDescription(String value) {
        return normalizeText(value)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeCategoryKey(String value) {
        if (value == null) {
            return "";
        }

        var clean = Normalizer.normalize(value.replace('\u00A0', ' ').trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return clean
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    public static boolean sameCategoryName(String left, String right) {
        return normalizeCategoryKey(left).equals(normalizeCategoryKey(right));
    }

    public static boolean sameCategoryKey(String left, String right) {
        return normalizeCategoryKey(left).equals(normalizeCategoryKey(right));
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}