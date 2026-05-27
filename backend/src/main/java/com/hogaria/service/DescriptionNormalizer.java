package com.hogaria.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DescriptionNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return "";
        }

        var clean = value
                .replace('\u00A0', ' ')
                .trim();

        clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return clean
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([|:/.-])\\s+", "$1")
                .replaceAll("([|:/.-])\\1{2,}", "$1")
                .trim();
    }

    public String normalizeNullable(String value) {
        var normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }
}
