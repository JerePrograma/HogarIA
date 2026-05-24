package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import com.hogaria.dto.QuickPlanTextDtos.*;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class QuickPlanTextImportService {
  private final FinancialProfileRepository profiles;
  private final MonthlyPlanService monthlyPlanService;
  private final MonthlyPlanItemRepository planRepo;
  private final CategoryRepository categoryRepository;
  private final QuickPlanTextParserService parser;
  private final QuickPlanClassificationService classification;

  public QuickPlanTextImportService(FinancialProfileRepository profiles, MonthlyPlanService monthlyPlanService, MonthlyPlanItemRepository planRepo, CategoryRepository categoryRepository, QuickPlanTextParserService parser, QuickPlanClassificationService classification) {
    this.profiles = profiles; this.monthlyPlanService = monthlyPlanService; this.planRepo = planRepo; this.categoryRepository = categoryRepository; this.parser = parser; this.classification = classification;
  }

  public QuickPlanTextPreviewResponse preview(UUID userId, UUID profileId, QuickPlanTextPreviewRequest request) {
    profiles.findByIdAndUserId(profileId, userId).orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    List<QuickPlanTextCandidate> candidates = new ArrayList<>();
    int y = request.periodYear(); int m = request.periodMonth();
    List<String> lines = Arrays.stream(request.rawText().split("\\R")).map(String::trim).filter(s -> !s.isBlank()).toList();
    int i = 1;
    for (String line : lines) {
      var parsed = parser.parseLine(line, request.defaultAmountScale(), request.approximateMargin());
      var cls = classification.classify(line);
      var category = findCategory(profileId, cls.categoryHint());
      List<String> warnings = new ArrayList<>();
      if (category == null && cls.categoryHint() != null) warnings.add("No se encontró categoría sugerida para " + cls.categoryHint());
      var item = new MonthlyPlanItemCreateRequest(cls.type(), parsed.title().isBlank() ? line : parsed.title(), null, null, y, m, parsed.amount(), parsed.minAmount(), parsed.maxAmount(), request.currency() == null ? "ARS" : request.currency(), null, null, null, null, null, cls.priority(), com.hogaria.entity.MonthlyPlanItem.Status.ESTIMATED, com.hogaria.entity.MonthlyPlanItem.Source.QUICK_CAPTURE, category == null ? null : category.getId(), null);
      boolean duplicate = isDuplicate(profileId, item);
      if (duplicate) warnings.add("Posible duplicado");
      candidates.add(new QuickPlanTextCandidate(i++, line, item, category == null ? null : category.getId(), category == null ? null : category.getName(), warnings, duplicate));
    }
    return new QuickPlanTextPreviewResponse(candidates, List.of());
  }

  public QuickPlanTextCommitResponse commit(UUID userId, UUID profileId, QuickPlanTextCommitRequest request) {
    profiles.findByIdAndUserId(profileId, userId).orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    List<MonthlyPlanItemResponse> created = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    for (var item : request.items()) {
      if (isDuplicate(profileId, item)) { warnings.add("Ítem omitido por duplicado: " + item.title()); continue; }
      created.add(monthlyPlanService.create(userId, profileId, item));
    }
    return new QuickPlanTextCommitResponse(created, warnings);
  }

  private com.hogaria.entity.Category findCategory(UUID profileId, String hint) {
    if (hint == null) return null;
    return categoryRepository.findByProfileIdAndActiveTrue(profileId).stream().filter(c -> c.getName() != null && c.getName().toLowerCase().contains(hint)).findFirst().orElseGet(() -> categoryRepository.findByProfileIdIsNullAndActiveTrue().stream().filter(c -> c.getName() != null && c.getName().toLowerCase().contains(hint)).findFirst().orElse(null));
  }

  private boolean isDuplicate(UUID profileId, MonthlyPlanItemCreateRequest item) {
    String normalized = normalize(item.title());
    return planRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, item.periodYear(), item.periodMonth()).stream().anyMatch(e -> normalize(e.getTitle()).equals(normalized) && Objects.equals(e.getAmount(), item.amount()) && Objects.equals(e.getMinAmount(), item.minAmount()) && Objects.equals(e.getMaxAmount(), item.maxAmount()));
  }
  private String normalize(String t){ return t == null ? "" : t.toLowerCase().replaceAll("[^\\p{L}\\p{N}]",""); }
}
