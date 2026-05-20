package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TransactionCategorySuggestionService {

  public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
  }

  public enum Status {
    READY,
    NEEDS_CATEGORY,
    NO_SUGGESTION,
    SKIPPED
  }

  public record Suggestion(
          UUID suggestedCategoryId,
          String suggestedCategoryName,
          Category.Type suggestedCategoryType,
          MoneyTransaction.MovementType suggestedMovementType,
          Confidence confidence,
          Status status,
          String reason,
          String warning,
          MoneyTransaction.PaymentChannel paymentChannel,
          MoneyTransaction.ClassificationStatus classificationStatus
  ) {
  }

  private record Rule(
          Pattern pattern,
          String categoryName,
          String categoryKey,
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType,
          Confidence confidence,
          MoneyTransaction.PaymentChannel paymentChannel,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String reason,
          String warning
  ) {
  }

  private final CategoryRepository categoryRepository;
  private final List<Rule> rules;

  public TransactionCategorySuggestionService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
    this.rules = List.of(
            rule(
                    "\\b(COMISION|COMISIÓN|CARGO|PUNTO\\s+EFECTIVO)\\b",
                    "Comisiones y cargos",
                    "comisiones_cargos",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.OTHER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_COMMISSION",
                    null
            ),
            rule(
                    "\\b(IMPUESTO|RG\\s*4815|IIBB|RETENCION|RETENCIÓN)\\b",
                    "Impuestos",
                    "impuestos",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.FIXED_EXPENSE,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.OTHER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_TAX",
                    null
            ),
            rule(
                    "\\b(SUBE)\\b",
                    "Transporte",
                    "transporte",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.MERCADO_PAGO,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_TRANSPORT_SUBE",
                    null
            ),
            rule(
                    "\\b(DB\\.DEBIN|DEBIN|CDNI|CUENTA\\s+DNI|PAGO\\s+DEBIN)\\b",
                    "Cuenta DNI / DEBIN",
                    "cuenta_dni_debin",
                    MoneyTransaction.MovementType.TRANSFER,
                    Category.Type.SAVING,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.DEBIN,
                    MoneyTransaction.ClassificationStatus.TECHNICAL,
                    "RULE_DEBIN_TECHNICAL",
                    "Movimiento técnico/canal. Revisar si es espejo de MercadoPago antes de computarlo como gasto."
            ),
            rule(
                    "\\b(TRANSF\\s+DE\\s+SYSTEMSCORP|SYSTEMSCORP|SUELDO|HABERES)\\b",
                    "Sueldo / Ingresos laborales",
                    "sueldo_ingresos_laborales",
                    MoneyTransaction.MovementType.INCOME,
                    Category.Type.INCOME,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_SALARY",
                    null
            ),
            rule(
                    "\\b(TRANSF\\s+DE|TRANSFERENCIA\\s+RECIBIDA|CR\\.TRAN\\.|BANK\\s+TRANSFER)\\b",
                    "Transferencias recibidas",
                    "transferencias_recibidas",
                    MoneyTransaction.MovementType.INCOME,
                    Category.Type.INCOME,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                    MoneyTransaction.ClassificationStatus.REVIEW,
                    "RULE_INCOMING_TRANSFER",
                    "Transferencia recibida: revisar si es ingreso real o movimiento entre cuentas propias."
            ),
            rule(
                    "\\b(LINK\\s+DE\\s+PAGO|PRESTAMOS|PRÉSTAMOS|MONEDA\\s+DIGITAL)\\b",
                    "CJ - Capital recuperado",
                    "cj_capital_recuperado",
                    MoneyTransaction.MovementType.SAVING,
                    Category.Type.INVESTMENT,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.MERCADO_PAGO,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_LOAN_CAPITAL_RECOVERY",
                    "Recupero de capital: no debería inflar ingresos operativos."
            ),
            rule(
                    "\\b(PAYMENT\\s+LINKED\\s+TO\\s+A\\s+LOAN\\s+ORIGINATION|MERCADOCREDITO|MERCADOCRÉDITO)\\b",
                    "Créditos y financiación",
                    "creditos_financiacion",
                    MoneyTransaction.MovementType.ADJUSTMENT,
                    Category.Type.DEBT,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.MERCADO_CREDITO,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_CREDIT_FINANCING",
                    "Financiación/deuda: no tratar como ingreso operativo."
            ),
            rule(
                    "\\b(PAGO\\s+CON\\s+TARJETA\\s+DEBITO|PAGO\\s+CON\\s+T\\.D\\.)\\b",
                    "Compras con tarjeta",
                    "compras_tarjeta",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.DEBIT_CARD,
                    MoneyTransaction.ClassificationStatus.REVIEW,
                    "RULE_DEBIT_CARD_PURCHASE",
                    "Puede ser consumo real o espejo de otro origen importado."
            )
    );
  }

  public Suggestion suggest(
          UUID profileId,
          String description,
          MoneyTransaction.MovementType movementType,
          String source,
          BigDecimal signedAmount
  ) {
    var text = normalize(description);

    if (text.contains("PENDIENTE DE LIQUIDACION")) {
      return new Suggestion(
              null,
              null,
              null,
              movementType,
              Confidence.LOW,
              Status.SKIPPED,
              "PENDIENTE_DE_LIQUIDACION",
              "Ruido de importación. No conviene impactarlo como movimiento real.",
              inferPaymentChannel(text, source),
              MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
      );
    }

    var categories = profileId == null
            ? List.<Category>of()
            : loadVisibleCategories(profileId);

    for (var rule : rules) {
      if (movementType != null && rule.movementType() != movementType && rule.movementType() != MoneyTransaction.MovementType.TRANSFER) {
        continue;
      }

      if (!rule.pattern().matcher(text).find()) {
        continue;
      }

      var category = findCategory(categories, rule.categoryName(), rule.categoryKey(), rule.movementType());

      if (category != null) {
        return new Suggestion(
                category.getId(),
                category.getName(),
                category.getType(),
                rule.movementType(),
                rule.confidence(),
                Status.READY,
                rule.reason(),
                rule.warning(),
                rule.paymentChannel(),
                rule.classificationStatus()
        );
      }

      return new Suggestion(
              null,
              rule.categoryName(),
              rule.categoryType(),
              rule.movementType(),
              Confidence.LOW,
              Status.NEEDS_CATEGORY,
              "CATEGORY_MISSING",
              "La regla matcheó, pero la categoría todavía no existe.",
              rule.paymentChannel(),
              MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
      );
    }

    return new Suggestion(
            null,
            null,
            null,
            movementType,
            Confidence.LOW,
            Status.NO_SUGGESTION,
            "NO_RULE",
            "No hay regla confiable para esta descripción.",
            inferPaymentChannel(text, source),
            MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
    );
  }

  private List<Category> loadVisibleCategories(UUID profileId) {
    var categories = new ArrayList<Category>();
    categories.addAll(categoryRepository.findByProfileIdAndActiveTrue(profileId));
    categories.addAll(categoryRepository.findByProfileIdIsNullAndActiveTrue());
    return categories;
  }

  private Category findCategory(
          List<Category> categories,
          String name,
          String key,
          MoneyTransaction.MovementType movementType
  ) {
    return categories
            .stream()
            .filter(category ->
                    same(category.getCategoryKey(), key)
                            || same(normalize(category.getName()), normalize(name))
            )
            .filter(category -> movementType == null || isCompatible(movementType, category.getType()))
            .findFirst()
            .orElse(null);
  }

  private boolean isCompatible(MoneyTransaction.MovementType movementType, Category.Type categoryType) {
    if (movementType == MoneyTransaction.MovementType.INCOME) {
      return categoryType == Category.Type.INCOME;
    }

    if (movementType == MoneyTransaction.MovementType.SAVING) {
      return categoryType == Category.Type.SAVING || categoryType == Category.Type.INVESTMENT;
    }

    if (movementType == MoneyTransaction.MovementType.EXPENSE) {
      return categoryType != Category.Type.INCOME;
    }

    return true;
  }

  private MoneyTransaction.PaymentChannel inferPaymentChannel(String normalizedText, String source) {
    var sourceValue = normalize(source);

    if (sourceValue.contains("MERCADO_PAGO") || sourceValue.contains("MERCADOPAGO")) {
      return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
    }

    if (normalizedText.contains("DEBIN")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (normalizedText.contains("CUENTA DNI") || normalizedText.contains("CDNI")) {
      return MoneyTransaction.PaymentChannel.CUENTA_DNI;
    }

    if (normalizedText.contains("TARJETA DEBITO") || normalizedText.contains("T.D.")) {
      return MoneyTransaction.PaymentChannel.DEBIT_CARD;
    }

    if (normalizedText.contains("MASTERCARD") || normalizedText.contains("VISA")) {
      return MoneyTransaction.PaymentChannel.CREDIT_CARD;
    }

    if (normalizedText.contains("TRANSFERENCIA") || normalizedText.contains("BANK TRANSFER")) {
      return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
    }

    return MoneyTransaction.PaymentChannel.UNKNOWN;
  }

  private Rule rule(
          String regex,
          String categoryName,
          String categoryKey,
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType,
          Confidence confidence,
          MoneyTransaction.PaymentChannel paymentChannel,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String reason,
          String warning
  ) {
    return new Rule(
            Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            categoryName,
            categoryKey,
            movementType,
            categoryType,
            confidence,
            paymentChannel,
            classificationStatus,
            reason,
            warning
    );
  }

  private boolean same(String left, String right) {
    return normalize(left).equals(normalize(right));
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }

    var clean = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");

    return clean
            .replace('\u00A0', ' ')
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
  }

}