package com.hogaria.service.transactionimport;

import com.hogaria.entity.MoneyTransaction;
import org.springframework.stereotype.Component;

@Component
public class PaymentChannelDetector {

  private final ImportTextNormalizer normalizer;

  public PaymentChannelDetector(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  public MoneyTransaction.PaymentChannel detectBancoProvincia(String description, String extendedDescription) {
    var text = normalizedJoin(description, extendedDescription);

    if (contains(text, "pago con t.d. en m.e.") || contains(text, "tarjeta debito m.e.")) {
      return MoneyTransaction.PaymentChannel.CARD_FOREIGN_CURRENCY;
    }

    if (contains(text, "debin")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (contains(text, "cuenta dni") || contains(text, "cdni")) {
      return MoneyTransaction.PaymentChannel.CUENTA_DNI;
    }

    if (contains(text, "tarjeta debito") || contains(text, "transporte con td")) {
      return MoneyTransaction.PaymentChannel.DEBIT_CARD;
    }

    if (contains(text, "pago directo")) {
      return MoneyTransaction.PaymentChannel.DIRECT_DEBIT;
    }

    if (contains(text, "cajero autom") || contains(text, "punto efectivo")) {
      return MoneyTransaction.PaymentChannel.ATM;
    }

    if (contains(text, "credito haberes")
            || contains(text, "transferencia")
            || contains(text, "traspaso")
            || contains(text, "acreditacion intereses")) {
      return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
    }

    return MoneyTransaction.PaymentChannel.UNKNOWN;
  }

  public MoneyTransaction.PaymentChannel detectMercadoPago(
          String detail,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String identificationNumber,
          String operationTags
  ) {
    var text = normalizedJoin(detail, operationType, paymentMethodType, paymentMethod, identificationNumber, operationTags);

    if (contains(text, "mercadocredito") || contains(text, "mercado credito") || contains(text, "loan origination")) {
      return MoneyTransaction.PaymentChannel.MERCADO_CREDITO;
    }

    if (contains(text, "emova") || contains(text, "sube") || contains(text, "pasajes")) {
      return MoneyTransaction.PaymentChannel.TRANSPORT_CARD;
    }

    if (contains(text, "qr")) {
      return MoneyTransaction.PaymentChannel.QR_PAYMENT;
    }

    if (contains(text, "transferencia bancaria") && contains(text, "debito inmediato")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (contains(text, "pago debin")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (contains(text, "bank transfer")
            || contains(text, "transferencia bancaria")
            || contains(text, "cuenta bancaria digital")) {
      return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
    }

    if (contains(text, "available money") || contains(text, "dinero disponible")) {
      return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
    }

    return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
  }

  private boolean contains(String haystack, String needle) {
    return haystack.contains(normalizer.normalize(needle));
  }

  private String normalizedJoin(String... values) {
    var builder = new StringBuilder();

    if (values != null) {
      for (var value : values) {
        var clean = normalizer.normalize(value);
        if (!clean.isBlank()) {
          if (!builder.isEmpty()) {
            builder.append(' ');
          }
          builder.append(clean);
        }
      }
    }

    return builder.toString();
  }
}
