package com.hogaria.service;

import com.hogaria.dto.BancoProvinciaLoanImportDtos.*;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import com.hogaria.entity.Category;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BancoProvinciaLoanImportService {
  public static final String LINEAR_WARNING = "Estimación lineal: Banco Provincia no informa en este archivo el sistema de amortización, tasa ni próxima cuota real.";
  private final FinancialProfileRepository profiles; private final BancoProvinciaLoanExcelParserService parser; private final MonthlyPlanItemRepository planRepo; private final MonthlyPlanService monthlyPlanService; private final CategoryRepository categoryRepository;
  public BancoProvinciaLoanImportService(FinancialProfileRepository profiles, BancoProvinciaLoanExcelParserService parser, MonthlyPlanItemRepository planRepo, MonthlyPlanService monthlyPlanService, CategoryRepository categoryRepository) {this.profiles = profiles; this.parser = parser; this.planRepo = planRepo; this.monthlyPlanService = monthlyPlanService; this.categoryRepository = categoryRepository;}

  public BancoProvinciaLoanPreviewResponse preview(UUID userId, UUID profileId, Integer periodYear, Integer periodMonth, String currency, MultipartFile file) {
    profiles.findByIdAndUserId(profileId, userId).orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    List<BancoProvinciaLoanCandidate> candidates = new ArrayList<>();
    for (var row : parser.parse(file)) {
      int months = monthsRemaining(periodYear, periodMonth, row.dueDate());
      List<String> warnings = new ArrayList<>(List.of(LINEAR_WARNING));
      if (months <= 0) warnings.add("Vencimiento ya alcanzado: revisar antes de crear compromiso automático.");
      var amount = months > 0 ? row.currentDebtAmount().divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
      var title = "Provincia préstamo " + row.accountNumber();
      boolean duplicate = isDuplicate(profileId, periodYear, periodMonth, row.accountNumber(), amount, title);
      if (duplicate) warnings.add("Posible duplicado en el período activo.");
      Category category = findDebtCategory(profileId);
      candidates.add(new BancoProvinciaLoanCandidate(row.lineNumber(), row.tipo(), row.identificacion(), row.accountNumber(), row.currentDebtAmount(), row.originalAmount(), row.dueDate(), months, amount, title, MonthlyPlanItem.Type.DEBT, MonthlyPlanItem.Priority.ESSENTIAL, MonthlyPlanItem.Status.ESTIMATED, duplicate, warnings, currency == null ? "ARS" : currency.toUpperCase(), category == null ? null : category.getId().toString()));
    }
    return new BancoProvinciaLoanPreviewResponse(candidates, List.of());
  }

  public BancoProvinciaLoanCommitResponse commit(UUID userId, UUID profileId, BancoProvinciaLoanCommitRequest request) {
    profiles.findByIdAndUserId(profileId, userId).orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    List<MonthlyPlanItemResponse> created = new ArrayList<>(); List<String> warnings = new ArrayList<>(); int skipped = 0;
    Category category = findDebtCategory(profileId);
    for (var c : request.candidates()) {
      String title = c.suggestedTitle() == null || c.suggestedTitle().isBlank() ? "Provincia préstamo " + c.accountNumber() : c.suggestedTitle();
      boolean dup = isDuplicate(profileId, request.periodYear(), request.periodMonth(), c.accountNumber(), c.estimatedMonthlyAmount(), title);
      if (request.skipDuplicates() && dup) { skipped++; warnings.add("Omitido duplicado: " + c.accountNumber()); continue; }
      if (c.monthsRemaining() == null || c.monthsRemaining() <= 0) { warnings.add("Revisión manual requerida por vencimiento para cuenta " + c.accountNumber()); skipped++; continue; }
      String desc = "Banco Provincia | cuenta " + c.accountNumber() + " | deuda actual " + c.currentDebtAmount() + " | importe original " + c.originalAmount() + " | vencimiento " + c.dueDate() + " | " + LINEAR_WARNING;
      var create = new MonthlyPlanItemCreateRequest(MonthlyPlanItem.Type.DEBT, title, desc, null, request.periodYear(), request.periodMonth(), c.estimatedMonthlyAmount(), null, null, "ARS", null, null, null, null, null, MonthlyPlanItem.Priority.ESSENTIAL, MonthlyPlanItem.Status.ESTIMATED, MonthlyPlanItem.Source.IMPORT, category == null ? null : category.getId(), null);
      created.add(monthlyPlanService.create(userId, profileId, create));
    }
    if (category == null) warnings.add("No se encontró categoría de deuda compatible; se crearon ítems sin categoría.");
    return new BancoProvinciaLoanCommitResponse(created, warnings, skipped);
  }

  private int monthsRemaining(int year, int month, LocalDate dueDate) { YearMonth due = YearMonth.from(dueDate); YearMonth base = YearMonth.of(year, month); return (due.getYear()-base.getYear())*12 + (due.getMonthValue()-base.getMonthValue()); }
  private Category findDebtCategory(UUID profileId) { var ks = List.of("deuda", "préstamo", "prestamo", "crédito", "credito", "financiación"); return categoryRepository.findByProfileIdAndActiveTrue(profileId).stream().filter(c -> { String n = c.getName()==null?"":c.getName().toLowerCase(); return ks.stream().anyMatch(n::contains);}).findFirst().orElse(null); }
  private boolean isDuplicate(UUID profileId, int y, int m, String account, BigDecimal amount, String title) { String normalized = normalize(title); return planRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, y, m).stream().anyMatch(i -> i.getSource()== MonthlyPlanItem.Source.IMPORT && normalize(i.getTitle()).equals(normalized) && Objects.equals(i.getAmount(), amount) && i.getDescription()!=null && i.getDescription().contains(account)); }
  private String normalize(String t){ return t==null?"":t.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", ""); }
}
