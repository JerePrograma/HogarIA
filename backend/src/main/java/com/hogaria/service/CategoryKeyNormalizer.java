package com.hogaria.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CategoryKeyNormalizer {

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        var clean = Normalizer.normalize(value.replace('\u00A0', ' ').trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");

        return clean.isBlank() ? null : clean;
    }
}
