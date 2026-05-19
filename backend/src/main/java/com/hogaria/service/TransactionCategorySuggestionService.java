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
  public enum Confidence { HIGH, MEDIUM, LOW }
  public enum Status { READY, NEEDS_CATEGORY, NO_SUGGESTION, SKIPPED }

  public record Suggestion(UUID suggestedCategoryId, String suggestedCategoryName, Category.Type suggestedCategoryType,
                           MoneyTransaction.MovementType suggestedMovementType, Confidence confidence, Status status,
                           String reason, String warning) {}

  private record Rule(Pattern pattern, String categoryName, MoneyTransaction.MovementType movementType, Category.Type categoryType, Confidence confidence) {}

  private final CategoryRepository categoryRepository;
  private final List<Rule> rules;

  public TransactionCategorySuggestionService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
    this.rules = List.of(
      rule("\\b(COMISION|COMISIÓN|CARGO|PUNTO\\s+EFECTIVO)\\b", "Comisiones y cargos", MoneyTransaction.MovementType.EXPENSE, Category.Type.VARIABLE_EXPENSE, Confidence.HIGH),
      rule("\\b(IMPUESTO|RG\\s*4815|IIBB|RETENCION|RETENCIÓN)\\b", "Impuestos", MoneyTransaction.MovementType.EXPENSE, Category.Type.FIXED_EXPENSE, Confidence.HIGH),
      rule("\\b(SUBE)\\b", "Transporte", MoneyTransaction.MovementType.EXPENSE, Category.Type.VARIABLE_EXPENSE, Confidence.HIGH),
      rule("\\b(DB\\.DEBIN|DEBIN|CDNI|CUENTA\\s+DNI|PAGO\\s+DEBIN)\\b", "Cuenta DNI / DEBIN", MoneyTransaction.MovementType.EXPENSE, Category.Type.VARIABLE_EXPENSE, Confidence.MEDIUM),
      rule("\\b(TRANSF\\s+DE\\s+SYSTEMSCORP|SYSTEMSCORP|SUELDO|HABERES)\\b", "Sueldo / Ingresos laborales", MoneyTransaction.MovementType.INCOME, Category.Type.INCOME, Confidence.HIGH),
      rule("\\b(TRANSF\\s+DE|TRANSFERENCIA\\s+RECIBIDA|CR\\.TRAN\\.|BANK\\s+TRANSFER)\\b", "Transferencias recibidas", MoneyTransaction.MovementType.INCOME, Category.Type.INCOME, Confidence.MEDIUM),
      rule("\\b(LINK\\s+DE\\s+PAGO|PRESTAMOS|PRÉSTAMOS)\\b", "CJ - Capital recuperado", MoneyTransaction.MovementType.SAVING, Category.Type.INVESTMENT, Confidence.MEDIUM),
      rule("\\b(PAYMENT\\s+LINKED\\s+TO\\s+A\\s+LOAN\\s+ORIGINATION|MERCADOCREDITO|MERCADOCRÉDITO)\\b", "Créditos y financiación", MoneyTransaction.MovementType.ADJUSTMENT, Category.Type.DEBT, Confidence.MEDIUM),
      rule("\\b(PAGO\\s+CON\\s+TARJETA\\s+DEBITO|PAGO\\s+CON\\s+T\\.D\\.)\\b", "Compras con tarjeta", MoneyTransaction.MovementType.EXPENSE, Category.Type.VARIABLE_EXPENSE, Confidence.MEDIUM)
    );
  }

  public Suggestion suggest(UUID profileId, String description, MoneyTransaction.MovementType movementType, String source, BigDecimal signedAmount) {
    var text = normalize(description);
    if (text.contains("PENDIENTE DE LIQUIDACION")) return new Suggestion(null,null,null,movementType,Confidence.LOW,Status.SKIPPED,"PENDIENTE_DE_LIQUIDACION","Ruido de importación");
    var categories = profileId == null ? List.<Category>of() : loadVisibleCategories(profileId);
    for (var r: rules) {
      if (r.movementType != null && movementType != null && r.movementType != movementType) continue;
      if (!r.pattern.matcher(text).find()) continue;
      var c = findCategory(categories, r.categoryName, movementType);
      if (c != null) return new Suggestion(c.getId(), c.getName(), c.getType(), r.movementType, r.confidence, Status.READY, "RULE_MATCH", null);
      return new Suggestion(null, r.categoryName, r.categoryType, r.movementType, Confidence.LOW, Status.NEEDS_CATEGORY, "CATEGORY_MISSING", null);
    }
    return new Suggestion(null, null, null, movementType, Confidence.LOW, Status.NO_SUGGESTION, "NO_RULE", null);
  }

  private List<Category> loadVisibleCategories(UUID profileId) { var l = new ArrayList<Category>(); l.addAll(categoryRepository.findByProfileIdAndActiveTrue(profileId)); l.addAll(categoryRepository.findByProfileIdIsNullAndActiveTrue()); return l; }
  private Category findCategory(List<Category> categories, String name, MoneyTransaction.MovementType mt){return categories.stream().filter(c->normalize(c.getName()).equals(normalize(name))).filter(c-> mt==null || isCompatible(mt,c.getType())).findFirst().orElse(null);}  
  private boolean isCompatible(MoneyTransaction.MovementType mt, Category.Type ct){ if(mt==MoneyTransaction.MovementType.INCOME) return ct==Category.Type.INCOME; if(mt==MoneyTransaction.MovementType.SAVING) return ct==Category.Type.SAVING||ct==Category.Type.INVESTMENT; if(mt==MoneyTransaction.MovementType.EXPENSE) return ct!=Category.Type.INCOME; return true; }
  private Rule rule(String p,String n,MoneyTransaction.MovementType mt,Category.Type ct,Confidence cf){ return new Rule(Pattern.compile(p),n,mt,ct,cf);}  
  private String normalize(String value){ if(value==null) return ""; var n=Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+",""); return n.toUpperCase(Locale.ROOT); }
}
