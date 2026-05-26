package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

  private record SuggestionContext(
          UUID profileId,
          String description,
          String normalizedDescription,
          MoneyTransaction.MovementType movementType,
          String source,
          String normalizedSource,
          BigDecimal amount
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
          String warning,
          boolean allowMovementTypeOverride
  ) {
  }

  private final CategoryRepository categoryRepository;
  private final List<Rule> rules;

  public TransactionCategorySuggestionService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
    this.rules = List.of(
            rule(
                    "\\b(COMISION|COMISIÓN|CARGO|PUNTO\\s+EFECTIVO|MANTENIMIENTO\\s+DE\\s+CUENTA)\\b",
                    "Comisiones y cargos",
                    "comisiones_y_cargos",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.OTHER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_COMMISSION",
                    null,
                    false
            ),
            rule(
                    "\\b(IMPUESTO|RG\\s*4815|IIBB|RETENCION|RETENCIÓN|PERCEPCION|PERCEPCIÓN|ARCA|AFIP|AGIP)\\b",
                    "Impuestos variables",
                    "impuestos_variables",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.OTHER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_TAX",
                    null,
                    false
            ),
            rule(
                    "\\b(SUBE|TRANSPORTE\\s+PUBLICO|TRANSPORTE\\s+PÚBLICO)\\b",
                    "Transporte público",
                    "transporte_publico",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.MERCADO_PAGO,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_TRANSPORT_SUBE",
                    null,
                    false
            ),
            rule(
                    "\\b(PAGO\\s+CON\\s+TARJETA\\s+DEBITO|PAGO\\s+CON\\s+T\\.D\\.|TARJETA\\s+DEBITO|TARJETA\\s+DÉBITO)\\b",
                    "Gastos generales",
                    "gastos_generales",
                    MoneyTransaction.MovementType.EXPENSE,
                    Category.Type.VARIABLE_EXPENSE,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.DEBIT_CARD,
                    MoneyTransaction.ClassificationStatus.REVIEW,
                    "RULE_DEBIT_CARD_PURCHASE_REVIEW",
                    "Compra con débito sin comercio confiable. Revisar antes de consolidar como gasto real.",
                    false
            ),
            rule(
                    "\\b(TRANSF\\s+DE\\s+SYSTEMSCORP|SYSTEMSCORP|SUELDO|HABERES)\\b",
                    "Sueldo",
                    "sueldo",
                    MoneyTransaction.MovementType.INCOME,
                    Category.Type.INCOME,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_SALARY",
                    null,
                    false
            ),
            rule(
                    "\\b(TRANSFERENCIA\\s+RECIBIDA|CR\\.TRAN\\.|BANK\\s+TRANSFER|TRANSF\\s+DE)\\b",
                    "Transferencias recibidas",
                    "transferencias_recibidas",
                    MoneyTransaction.MovementType.INCOME,
                    Category.Type.INCOME,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                    MoneyTransaction.ClassificationStatus.REVIEW,
                    "RULE_INCOMING_TRANSFER_REVIEW",
                    "Transferencia recibida: revisar si es ingreso real o movimiento entre cuentas propias.",
                    false
            ),
            rule(
                    "\\b(PR[EÉ]STAMO\\s+CJ|CJ\\s*-\\s*CAPITAL\\s+PRESTADO)\\b",
                    "CJ - Capital prestado",
                    "cj_capital_prestado",
                    MoneyTransaction.MovementType.ADJUSTMENT,
                    Category.Type.INVESTMENT,
                    Confidence.HIGH,
                    MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "CJPRESTAMOS_DISBURSEMENT",
                    "Capital prestado recuperable: no debería tratarse como gasto de consumo.",
                    true
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
                    "Recupero de capital: no debería inflar ingresos operativos.",
                    true
            ),
            rule(
                    "\\b(PAYMENT\\s+LINKED\\s+TO\\s+A\\s+LOAN\\s+ORIGINATION|MERCADOCREDITO|MERCADOCRÉDITO)\\b",
                    "Créditos y financiación",
                    "creditos_y_financiacion",
                    MoneyTransaction.MovementType.ADJUSTMENT,
                    Category.Type.DEBT,
                    Confidence.MEDIUM,
                    MoneyTransaction.PaymentChannel.MERCADO_CREDITO,
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_CREDIT_FINANCING",
                    "Financiación/deuda: no tratar como ingreso operativo.",
                    true
            )
    );
  }

  public Suggestion suggest(
          UUID profileId,
          String description,
          MoneyTransaction.MovementType movementType,
          String source,
          BigDecimal amount
  ) {
    var context = new SuggestionContext(
            profileId,
            description,
            normalize(description),
            movementType,
            source,
            normalize(source),
            amount
    );

    if (context.normalizedDescription().contains("PENDIENTE DE LIQUIDACION")) {
      return new Suggestion(
              null,
              null,
              null,
              movementType,
              Confidence.LOW,
              Status.SKIPPED,
              "PENDIENTE_DE_LIQUIDACION",
              "Ruido de importación. No conviene impactarlo como movimiento real.",
              inferPaymentChannel(context.normalizedDescription(), context.normalizedSource()),
              MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
      );
    }

    var technicalSuggestion = suggestTechnicalChannel(context);
    if (technicalSuggestion != null) {
      return technicalSuggestion;
    }

    var categories = profileId == null
            ? List.<Category>of()
            : loadVisibleCategories(profileId);

    for (var rule : rules) {
      if (!rule.allowMovementTypeOverride()
              && movementType != null
              && rule.movementType() != movementType) {
        continue;
      }

      if (!rule.pattern().matcher(context.normalizedDescription()).find()) {
        continue;
      }

      var category = findCategory(
              categories,
              rule.categoryName(),
              rule.categoryKey(),
              rule.movementType(),
              rule.categoryType()
      );

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
              "La regla matcheó, pero la categoría global/perfil no existe o no es compatible.",
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
            inferPaymentChannel(context.normalizedDescription(), context.normalizedSource()),
            MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
    );
  }

  private Suggestion suggestTechnicalChannel(SuggestionContext context) {
    var text = context.normalizedDescription();

    boolean debin = text.contains("DEBIN");
    boolean cuentaDni = text.contains("CUENTA DNI") || text.contains("CDNI");

    if (!debin && !cuentaDni) {
      return null;
    }

    var paymentChannel = debin
            ? MoneyTransaction.PaymentChannel.DEBIN
            : MoneyTransaction.PaymentChannel.CUENTA_DNI;

    /*
     * Regla deliberadamente conservadora:
     * - Si viene como EXPENSE y solo dice DEBIN/CUENTA DNI, no se puede inferir gasto real.
     * - Se propone revisión, no CLASSIFIED.
     * - Si el usuario quiere tratarlo como técnico, puede aplicar targetMovementType=TRANSFER.
     */
    if (context.movementType() == MoneyTransaction.MovementType.EXPENSE) {
      return new Suggestion(
              null,
              null,
              null,
              context.movementType(),
              Confidence.LOW,
              Status.NEEDS_CATEGORY,
              "RULE_DEBIN_CDNI_EXPENSE_AMBIGUOUS",
              "DEBIN/Cuenta DNI identifica canal de pago, no categoría económica. Requiere revisión o asignación manual por lote.",
              paymentChannel,
              MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
      );
    }

    var categories = context.profileId() == null
            ? List.<Category>of()
            : loadVisibleCategories(context.profileId());

    var category = findCategory(
            categories,
            "Cuenta DNI / DEBIN",
            "cuenta_dni_debin",
            MoneyTransaction.MovementType.TRANSFER,
            Category.Type.SAVING
    );

    if (category == null) {
      return new Suggestion(
              null,
              "Cuenta DNI / DEBIN",
              Category.Type.SAVING,
              MoneyTransaction.MovementType.TRANSFER,
              Confidence.LOW,
              Status.NEEDS_CATEGORY,
              "CATEGORY_MISSING",
              "La regla técnica matcheó, pero falta la categoría Cuenta DNI / DEBIN.",
              paymentChannel,
              MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
      );
    }

    return new Suggestion(
            category.getId(),
            category.getName(),
            category.getType(),
            MoneyTransaction.MovementType.TRANSFER,
            Confidence.MEDIUM,
            Status.READY,
            "RULE_DEBIN_CDNI_TECHNICAL",
            "Movimiento técnico/canal. Revisar si es espejo de otro origen antes de computarlo.",
            paymentChannel,
            MoneyTransaction.ClassificationStatus.TECHNICAL
    );
  }

  private List<Category> loadVisibleCategories(UUID profileId) {
    var categories = new ArrayList<Category>();
    categories.addAll(categoryRepository.findByProfileIdAndActiveTrue(profileId));
    categories.addAll(categoryRepository.findByProfileIdIsNullAndActiveTrue());

    categories.sort(Comparator
            .comparing((Category category) -> category.getProfileId() == null ? 1 : 0)
            .thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER));

    return categories;
  }

  private Category findCategory(
          List<Category> categories,
          String name,
          String key,
          MoneyTransaction.MovementType movementType,
          Category.Type expectedType
  ) {
    return categories
            .stream()
            .filter(Category::getActive)
            .filter(category -> expectedType == null || category.getType() == expectedType)
            .filter(category -> sameKey(category.getCategoryKey(), key)
                    || sameName(category.getName(), name))
            .filter(category -> movementType == null || isCompatible(movementType, category.getType()))
            .findFirst()
            .orElse(null);
  }

  private boolean isCompatible(
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType
  ) {
    if (movementType == null || categoryType == null) {
      return false;
    }

    if (movementType == MoneyTransaction.MovementType.INCOME) {
      return categoryType == Category.Type.INCOME;
    }

    if (movementType == MoneyTransaction.MovementType.SAVING) {
      return categoryType == Category.Type.SAVING
              || categoryType == Category.Type.INVESTMENT;
    }

    if (movementType == MoneyTransaction.MovementType.EXPENSE) {
      return categoryType == Category.Type.FIXED_EXPENSE
              || categoryType == Category.Type.VARIABLE_EXPENSE
              || categoryType == Category.Type.DEBT;
    }

    if (movementType == MoneyTransaction.MovementType.TRANSFER) {
      return categoryType == Category.Type.SAVING
              || categoryType == Category.Type.INVESTMENT
              || categoryType == Category.Type.VARIABLE_EXPENSE;
    }

    if (movementType == MoneyTransaction.MovementType.ADJUSTMENT) {
      return categoryType != Category.Type.INCOME;
    }

    return false;
  }

  private MoneyTransaction.PaymentChannel inferPaymentChannel(
          String normalizedText,
          String normalizedSource
  ) {
    if (normalizedSource.contains("MERCADO_PAGO") || normalizedSource.contains("MERCADOPAGO")) {
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
          String warning,
          boolean allowMovementTypeOverride
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
            warning,
            allowMovementTypeOverride
    );
  }

  private boolean sameKey(String left, String right) {
    return normalizeKey(left).equals(normalizeKey(right));
  }

  private boolean sameName(String left, String right) {
    return normalize(left).equals(normalize(right));
  }

  private String normalizeKey(String value) {
    return normalize(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
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