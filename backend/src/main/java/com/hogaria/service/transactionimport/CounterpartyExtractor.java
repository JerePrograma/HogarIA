package com.hogaria.service.transactionimport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CounterpartyExtractor {

  private static final Pattern NAME_PATTERN = Pattern.compile(
          "\\bN:([^\\r\\n]+?)(?:\\s+(?:CA:|C:|D:|ID\\.|REF\\.|VAR\\b)|$)",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );
  private static final Pattern PARENTHESIS_DOCUMENT_PATTERN = Pattern.compile("\\((\\d{7,})\\)");
  private static final Pattern CD_DOCUMENT_PATTERN = Pattern.compile("\\b[CD]:\\s*(\\d{7,})\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern ID_DOCUMENT_PATTERN = Pattern.compile("\\bID\\.?\\s*([A-Z0-9.-]{6,})\\b", Pattern.CASE_INSENSITIVE);

  private final ImportTextNormalizer normalizer;

  public CounterpartyExtractor(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  public ExtractedCounterparty fromBancoProvincia(String extendedDescription, MerchantExtractor.ExtractedMerchant merchant) {
    if (merchant != null && merchant.raw() != null && !merchant.raw().isBlank()) {
      return new ExtractedCounterparty(merchant.raw(), null);
    }

    var extended = normalizer.cleanRaw(extendedDescription);

    if (extended.isBlank()) {
      return ExtractedCounterparty.empty();
    }

    var nameMatcher = NAME_PATTERN.matcher(extended);
    String rawName = nameMatcher.find() ? normalizer.cleanRaw(nameMatcher.group(1)) : null;
    var document = firstMatch(extended, PARENTHESIS_DOCUMENT_PATTERN, CD_DOCUMENT_PATTERN, ID_DOCUMENT_PATTERN);

    if (rawName != null && !rawName.isBlank()) {
      return new ExtractedCounterparty(rawName, hashDocument(document));
    }

    return document == null
            ? ExtractedCounterparty.empty()
            : new ExtractedCounterparty(null, hashDocument(document));
  }

  public ExtractedCounterparty fromMercadoPago(String payer, String bank, String wallet, String identificationNumber) {
    var rawName = firstNonBlank(payer, bank, wallet);
    var documentHash = hashDocument(identificationNumber);

    return rawName.isBlank() && documentHash == null
            ? ExtractedCounterparty.empty()
            : new ExtractedCounterparty(rawName.isBlank() ? null : rawName, documentHash);
  }

  private String firstMatch(String value, Pattern... patterns) {
    for (var pattern : patterns) {
      var matcher = pattern.matcher(value);
      if (matcher.find()) {
        return normalizer.cleanRaw(matcher.group(1));
      }
    }

    return null;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }

    for (var value : values) {
      var clean = normalizer.cleanRaw(value);
      if (!clean.isBlank()) {
        return clean;
      }
    }

    return "";
  }

  private String hashDocument(String document) {
    var clean = normalizer.cleanRaw(document).replaceAll("[^A-Za-z0-9]", "");

    if (clean.isBlank()) {
      return null;
    }

    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var bytes = digest.digest(clean.getBytes(StandardCharsets.UTF_8));
      var builder = new StringBuilder();

      for (byte current : bytes) {
        builder.append(String.format("%02x", current));
      }

      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo hashear documento de contraparte", ex);
    }
  }

  public record ExtractedCounterparty(String raw, String documentHash) {
    static ExtractedCounterparty empty() {
      return new ExtractedCounterparty(null, null);
    }
  }
}
