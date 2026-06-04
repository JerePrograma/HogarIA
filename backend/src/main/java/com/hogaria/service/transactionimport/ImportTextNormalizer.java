package com.hogaria.service.transactionimport;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ImportTextNormalizer {

  public String cleanRaw(String value) {
    if (value == null) {
      return "";
    }

    var clean = value
            .replace('\u00A0', ' ')
            .replace('\uFEFF', ' ')
            .replace('\u0000', ' ')
            .trim();

    clean = clean.replaceAll("^\"{2,}", "\"").replaceAll("\"{2,}$", "\"");

    while (clean.length() >= 2 && clean.startsWith("\"") && clean.endsWith("\"")) {
      clean = clean.substring(1, clean.length() - 1).trim();
    }

    return clean.replaceAll("\\s+", " ").trim();
  }

  public String normalize(String value) {
    var clean = cleanRaw(value);

    if (clean.isBlank()) {
      return "";
    }

    clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");

    return clean
            .toLowerCase(Locale.ROOT)
            .replaceAll("[\\t\\r\\n]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
  }

  public String canonicalHeader(String value) {
    return normalize(value)
            .replaceAll("[^a-z0-9]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
  }

  public boolean sameCanonical(String left, String right) {
    return canonicalHeader(left).equals(canonicalHeader(right));
  }
}
