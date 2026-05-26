package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import com.hogaria.dto.QuickPlanTextDtos.*;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class QuickPlanTextImportService {
  private final FinancialProfileRepository profiles; private final MonthlyPlanService monthlyPlanService; private final MonthlyPlanItemRepository planRepo; private final CategoryRepository categoryRepository; private final QuickPlanTextParserService parser; private final QuickPlanClassificationService classification;
  public QuickPlanTextImportService(FinancialProfileRepository profiles, MonthlyPlanService monthlyPlanService, MonthlyPlanItemRepository planRepo, CategoryRepository categoryRepository, QuickPlanTextParserService parser, QuickPlanClassificationService classification) {this.profiles = profiles; this.monthlyPlanService = monthlyPlanService; this.planRepo = planRepo; this.categoryRepository = categoryRepository; this.parser = parser; this.classification = classification;}

  public QuickPlanTextPreviewResponse preview(UUID userId, UUID profileId, QuickPlanTextPreviewRequest request) {
    profiles.findByIdAndUserId(profileId, userId).orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    List<QuickPlanTextCandidate> candidates = new ArrayList<>();
    int y = request.periodYear(); int m = request.periodMonth();
    List<String> lines = Arrays.stream(request.rawText().split("\\R")).map(String::trim).filter(s -> !s.isBlank()).toList();
    Set<String> seenInPreview = new HashSet<>();
    int i = 1;
    for (String line : lines) {
      var parsed = parser.parseLine(line, request.defaultAmountScale(), request.approximateMargin(), y, m);
      var cls = classification.classify(line);
      var category = findCategory(profileId, cls.categoryHint());
      List<String> warnings = new ArrayList<>();
      if (parsed.amount()==null && parsed.minAmount()==null) warnings.add("La línea no tiene monto detectable");
      if (parsed.invalidDate()) warnings.add("La fecha detectada no es válida; se usará el período seleccionado");
      if (category == null && cls.categoryHint() != null) warnings.add("No se encontró categoría sugerida para " + cls.categoryHint());
      YearMonth candidatePeriod = parsed.expectedDate() == null ? YearMonth.of(y, m) : YearMonth.from(parsed.expectedDate());
      if (parsed.expectedDate() != null && (candidatePeriod.getYear() != y || candidatePeriod.getMonthValue() != m)) {
        warnings.add("Se creará en " + candidatePeriod.getMonthValue() + "/" + candidatePeriod.getYear() + " porque la fecha esperada cae en ese período");
      }
      var c = new NormalizedCandidate(i, parsed.title().isBlank()?line:parsed.title(), cls.type(), cls.priority(), parsed.expectedDate(), candidatePeriod.getYear(), candidatePeriod.getMonthValue(), parsed.amount(), parsed.minAmount(), parsed.maxAmount(), category == null ? null : category.getId(), null);
      boolean duplicate = isDuplicate(profileId, c.periodYear(), c.periodMonth(), c) || !seenInPreview.add(duplicateKey(c.periodYear(), c.periodMonth(), c));
      if (duplicate) warnings.add("Posible duplicado");
      candidates.add(new QuickPlanTextCandidate(i++, line, c, category == null ? null : category.getId(), category == null ? null : category.getName(), warnings, duplicate));
    }
    return new QuickPlanTextPreviewResponse(candidates, List.of());
  }

  public QuickPlanTextCommitResponse commit(UUID userId, UUID profileId, QuickPlanTextCommitRequest request) {
    profiles.findByIdAndUserId(profileId, userId).orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    List<MonthlyPlanItemResponse> created = new ArrayList<>(); List<String> warnings = new ArrayList<>(); int skipped=0;
    Set<String> seenInCommit = new HashSet<>();
    for (var c : request.candidates()) {
      YearMonth targetPeriod = resolveCandidatePeriod(c, request);
      if (isDuplicate(profileId, targetPeriod.getYear(), targetPeriod.getMonthValue(), c) || !seenInCommit.add(duplicateKey(targetPeriod.getYear(), targetPeriod.getMonthValue(), c))) { warnings.add("Ítem omitido por duplicado: " + c.title()); skipped++; continue; }
      var item = new MonthlyPlanItemCreateRequest(c.type(), c.title(), null, c.expectedDate(), targetPeriod.getYear(), targetPeriod.getMonthValue(), c.amount(), c.minAmount(), c.maxAmount(), "ARS", null, null, null, null, null, c.priority(), com.hogaria.entity.MonthlyPlanItem.Status.ESTIMATED, com.hogaria.entity.MonthlyPlanItem.Source.QUICK_CAPTURE, c.categoryId(), c.accountId());
      created.add(monthlyPlanService.create(userId, profileId, item));
    }
    return new QuickPlanTextCommitResponse(created, warnings, skipped);
  }
  private com.hogaria.entity.Category findCategory(UUID profileId, String hint) { if (hint == null) return null; return categoryRepository.findByProfileIdAndActiveTrue(profileId).stream().filter(c -> c.getName() != null && c.getName().toLowerCase().contains(hint)).findFirst().orElse(null); }
  private boolean isDuplicate(UUID profileId, int y, int m, NormalizedCandidate item) { String normalized = normalize(item.title()); return planRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, y, m).stream().anyMatch(e -> normalize(e.getTitle()).equals(normalized)); }
  private String normalize(String t){ return t == null ? "" : t.toLowerCase().replaceAll("[^\\p{L}\\p{N}]",""); }
  private String duplicateKey(int y, int m, NormalizedCandidate item) { return y + "-" + m + "-" + normalize(item.title()); }
  private YearMonth resolveCandidatePeriod(NormalizedCandidate c, QuickPlanTextCommitRequest request) {
    if (c.periodYear() != null && c.periodMonth() != null) return YearMonth.of(c.periodYear(), c.periodMonth());
    if (c.expectedDate() != null) return YearMonth.from(c.expectedDate());
    if (request.periodYear() != null && request.periodMonth() != null) return YearMonth.of(request.periodYear(), request.periodMonth());
    throw new BadRequestException("Cada candidato debe tener período operativo o fecha esperada");
  }
}
