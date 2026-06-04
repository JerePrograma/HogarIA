package com.hogaria.service.transactionimport;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MerchantExtractor {

  private static final Pattern CDNI_COMPRA_PATTERN = Pattern.compile(
          "\\b(?:COMPRA|PAGO)\\s+CT(?:\\s+CDNI)?\\s+\\d{1,2}/\\d{1,2}\\s+(.+?)(?:\\s+\\(|\\s+AUT\\b|$)",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  private static final Set<String> GENERIC_MERCADO_PAGO_DETAILS = Set.of(
          "pago debin",
          "bank transfer",
          "link de pago",
          "varios",
          "var",
          "orden de venta",
          "se acredito dinero",
          "se acreditó dinero",
          "payment linked to a loan origination",
          "pago aprobado"
  );

  private final ImportTextNormalizer normalizer;

  public MerchantExtractor(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  public ExtractedMerchant fromBancoProvincia(
          String description,
          String extendedDescription,
          String merchantName
  ) {
    var explicitMerchant = normalizer.cleanRaw(merchantName);

    if (!explicitMerchant.isBlank()) {
      return merchant(explicitMerchant);
    }

    var extended = normalizer.cleanRaw(extendedDescription);
    var matcher = CDNI_COMPRA_PATTERN.matcher(extended);

    if (matcher.find()) {
      return merchant(matcher.group(1));
    }

    return ExtractedMerchant.empty();
  }

  public ExtractedMerchant fromMercadoPago(
          String detail,
          String identificationNumber,
          String operationTags,
          String payer
  ) {
    var identification = normalizer.cleanRaw(identificationNumber);
    var tags = normalizer.cleanRaw(operationTags);

    if (contains(identification, "EMOVA") || contains(tags, "EMOVA")) {
      return merchant("EMOVA");
    }

    var cleanDetail = normalizer.cleanRaw(detail);
    var normalizedDetail = normalizer.normalize(cleanDetail);

    if (!cleanDetail.isBlank() && !GENERIC_MERCADO_PAGO_DETAILS.contains(normalizedDetail)) {
      return merchant(cleanDetail);
    }

    var cleanPayer = normalizer.cleanRaw(payer);
    if (contains(cleanPayer, "MERCADOCREDITO") || contains(cleanPayer, "MERCADO CREDITO")) {
      return merchant("Mercado Crédito");
    }

    return ExtractedMerchant.empty();
  }

  private ExtractedMerchant merchant(String raw) {
    var clean = normalizer.cleanRaw(raw);
    return clean.isBlank()
            ? ExtractedMerchant.empty()
            : new ExtractedMerchant(clean, normalizer.normalize(clean));
  }

  private boolean contains(String value, String needle) {
    return normalizer.normalize(value).contains(normalizer.normalize(needle));
  }

  public record ExtractedMerchant(String raw, String normalized) {
    static ExtractedMerchant empty() {
      return new ExtractedMerchant(null, null);
    }
  }
}
