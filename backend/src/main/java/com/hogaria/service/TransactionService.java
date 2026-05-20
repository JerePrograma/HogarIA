package com.hogaria.service;

import com.hogaria.dto.BulkRecategorizeApplyRequest;
import com.hogaria.dto.BulkRecategorizeApplyResponse;
import com.hogaria.dto.BulkRecategorizeApplyItem;
import com.hogaria.dto.BulkRecategorizeCandidate;
import com.hogaria.dto.BulkRecategorizePreviewRequest;
import com.hogaria.dto.BulkRecategorizePreviewResponse;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionResponse;
import com.hogaria.dto.TransactionUpdateRequest;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

  private final MoneyTransactionRepository repository;
  private final FinancialProfileRepository profileRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final TransactionCategorySuggestionService suggestionService;

  public TransactionService(
          MoneyTransactionRepository repository,
          FinancialProfileRepository profileRepository,
          AccountRepository accountRepository,
          CategoryRepository categoryRepository,
          TransactionCategorySuggestionService suggestionService
  ) {
    this.repository = repository;
    this.profileRepository = profileRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.suggestionService = suggestionService;
  }

  @Transactional
  public TransactionResponse create(TransactionCreateRequest request, UUID userId) {
    return create(request, userId, null);
  }

  @Transactional
  public TransactionResponse create(
          TransactionCreateRequest request,
          UUID userId,
          TransactionMetadata metadata
  ) {
    if (request.amount().signum() <= 0) {
      throw new BadRequestException("Amount must be positive");
    }

    validate(
            request.profileId(),
            request.accountId(),
            request.categoryId(),
            request.movementType(),
            userId
    );

    var classificationStatus = metadata != null && metadata.classificationStatus() != null
            ? metadata.classificationStatus()
            : request.categoryId() == null
              ? MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
              : MoneyTransaction.ClassificationStatus.CLASSIFIED;

    var transaction = MoneyTransaction.builder()
            .profileId(request.profileId())
            .accountId(request.accountId())
            .categoryId(request.categoryId())
            .movementType(request.movementType())
            .realDate(request.realDate())
            .budgetDate(request.budgetDate())
            .amount(request.amount())
            .currency(request.currency().toUpperCase())
            .description(request.description())
            .origin(request.origin() == null ? MoneyTransaction.Origin.MANUAL : request.origin())
            .status(request.status() == null ? MoneyTransaction.Status.CONFIRMED : request.status())
            .source(metadata == null ? null : metadata.source())
            .sourceOperationId(metadata == null ? null : metadata.sourceOperationId())
            .sourceHash(metadata == null ? null : metadata.sourceHash())
            .paymentChannel(metadata == null ? null : metadata.paymentChannel())
            .counterparty(metadata == null ? null : metadata.counterparty())
            .classificationStatus(classificationStatus)
            .classificationReason(metadata == null ? null : metadata.classificationReason())
            .importBatchId(metadata == null ? null : metadata.importBatchId())
            .internalTransferGroupId(metadata == null ? null : metadata.internalTransferGroupId())
            .build();

    return toResponse(repository.save(transaction));
  }

  @Transactional(readOnly = true)
  public List<TransactionResponse> list(UUID userId, UUID profileId, int year, int month) {
    ensureProfile(profileId, userId);

    var from = LocalDate.of(year, month, 1);
    var to = from.withDayOfMonth(from.lengthOfMonth());

    return repository.findByProfileIdAndBudgetDateBetween(profileId, from, to)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public TransactionResponse get(UUID userId, UUID txId) {
    var transaction = repository.findById(txId)
            .orElseThrow(() -> new NotFoundException("Transaction not found"));

    ensureProfile(transaction.getProfileId(), userId);

    return toResponse(transaction);
  }

  @Transactional
  public TransactionResponse update(UUID userId, UUID txId, TransactionUpdateRequest request) {
    var transaction = repository.findById(txId)
            .orElseThrow(() -> new NotFoundException("Transaction not found"));

    ensureProfile(transaction.getProfileId(), userId);

    if (request.amount() != null && request.amount().signum() <= 0) {
      throw new BadRequestException("Amount must be positive");
    }

    if (request.accountId() != null) {
      transaction.setAccountId(request.accountId());
    }

    if (request.categoryId() != null) {
      transaction.setCategoryId(request.categoryId());
    }

    if (request.movementType() != null) {
      transaction.setMovementType(request.movementType());
    }

    if (request.realDate() != null) {
      transaction.setRealDate(request.realDate());
    }

    if (request.budgetDate() != null) {
      transaction.setBudgetDate(request.budgetDate());
    }

    if (request.amount() != null) {
      transaction.setAmount(request.amount());
    }

    if (request.currency() != null) {
      transaction.setCurrency(request.currency().toUpperCase());
    }

    if (request.description() != null) {
      transaction.setDescription(request.description());
    }

    if (request.origin() != null) {
      transaction.setOrigin(request.origin());
    }

    if (request.status() != null) {
      transaction.setStatus(request.status());
    }

    validate(
            transaction.getProfileId(),
            transaction.getAccountId(),
            transaction.getCategoryId(),
            transaction.getMovementType(),
            userId
    );

    return toResponse(repository.save(transaction));
  }

  @Transactional
  public void delete(UUID userId, UUID txId) {
    var transaction = repository.findById(txId)
            .orElseThrow(() -> new NotFoundException("Transaction not found"));

    ensureProfile(transaction.getProfileId(), userId);

    repository.delete(transaction);
  }

  @Transactional(readOnly = true)
  public BulkRecategorizePreviewResponse previewBulkRecategorize(
          UUID userId,
          UUID profileId,
          BulkRecategorizePreviewRequest request
  ) {
    ensureProfile(profileId, userId);

    var warnings = new ArrayList<String>();
    var errors = new ArrayList<String>();

    var targetMode = normalizeTargetMode(request.targetMode());
    if (isManualMode(targetMode) && request.toCategoryId() == null) {
      errors.add("Falta toCategoryId.");
      return emptyBulkPreview(profileId, null, errors);
    }

    Category targetCategory = isManualMode(targetMode) ? getCategoryForProfile(profileId, request.toCategoryId()) : null;

    LocalDate from = request.from() != null ? request.from() : LocalDate.of(1900, 1, 1);
    LocalDate to = request.to() != null ? request.to() : LocalDate.of(2999, 12, 31);

    if (from.isAfter(to)) {
      errors.add("El rango de fechas es inválido: from no puede ser posterior a to.");
      return emptyBulkPreview(profileId, targetCategory == null ? null : targetCategory.getId(), errors);
    }

    List<MoneyTransaction> baseTransactions = loadBulkBaseTransactions(profileId, request, from, to);

    var filtered = baseTransactions.stream()
            .filter(transaction -> matchesBulkFilters(transaction, request))
            .toList();

    var ambiguityCountByKey = buildAmbiguityCountByKey(filtered);

    var candidates = new ArrayList<BulkRecategorizeCandidate>();

    int updatableCount = 0;
    int ambiguousCount = 0;
    int skippedCount = 0;

    for (var transaction : filtered) {
      String previewStatus = "READY";
      String warning = "";

      var key = ambiguityKey(transaction);
      boolean ambiguous = ambiguityCountByKey.getOrDefault(key, 0) > 1;

      UUID candidateTargetCategoryId = targetCategory != null ? targetCategory.getId() : null;
      String candidateTargetCategoryName = targetCategory != null ? targetCategory.getName() : null;
      if (isAutoMode(targetMode)) {
        var suggestion = suggestionService.suggest(
                profileId,
                transaction.getDescription(),
                transaction.getMovementType(),
                transaction.getSource(),
                transaction.getAmount()
        );

        candidateTargetCategoryId = suggestion.suggestedCategoryId();
        candidateTargetCategoryName = suggestion.suggestedCategoryName();

        if (suggestion.warning() != null && !suggestion.warning().isBlank()) {
          warning = suggestion.warning();
        }
      }

      if (candidateTargetCategoryId == null) {
        previewStatus = "NEEDS_CATEGORY";
        warning = "No se pudo resolver categoría destino automáticamente.";
        skippedCount++;
      } else if (Objects.equals(transaction.getCategoryId(), candidateTargetCategoryId)) {
        previewStatus = "SKIPPED";
        warning = "Ya tiene esa categoría.";
        skippedCount++;
      } else if (!isMovementCategoryCompatible(transaction.getMovementType(), getCategoryForProfile(profileId, candidateTargetCategoryId).getType())) {
        previewStatus = "SKIPPED";
        warning = "La categoría destino no es compatible con el tipo de movimiento.";
        skippedCount++;
      } else if (ambiguous) {
        previewStatus = "AMBIGUOUS";
        warning = "Hay más de un movimiento con la misma cuenta, fecha y monto. Requiere revisión manual.";
        ambiguousCount++;
      } else {
        updatableCount++;
      }

      candidates.add(toBulkCandidate(
              transaction,
              candidateTargetCategoryId,
              candidateTargetCategoryName,
              previewStatus,
              warning
      ));
    }

    if (filtered.isEmpty()) {
      warnings.add("No se encontraron movimientos con los filtros indicados.");
    }

    return new BulkRecategorizePreviewResponse(
            profileId,
            targetCategory == null ? null : targetCategory.getId(),
            filtered.size(),
            updatableCount,
            ambiguousCount,
            skippedCount,
            candidates,
            warnings,
            errors
    );
  }

  @Transactional
  public BulkRecategorizeApplyResponse applyBulkRecategorize(
          UUID userId,
          UUID profileId,
          BulkRecategorizeApplyRequest request
  ) {
    ensureProfile(profileId, userId);

    var targetMode = normalizeTargetMode(request.targetMode());

    int updated = 0;
    int skipped = 0;
    int failed = 0;

    var updatedIds = new ArrayList<UUID>();
    var warnings = new ArrayList<String>();
    var errors = new ArrayList<String>();

    List<BulkRecategorizeApplyItem> items = new ArrayList<>();
    if (isManualMode(targetMode)) {
      var targetCategory = getCategoryForProfile(profileId, request.toCategoryId());
      for (var transactionId : request.transactionIds()) {
        items.add(new BulkRecategorizeApplyItem(transactionId, targetCategory.getId()));
      }
    } else {
      if (request.updates() != null) {
        items.addAll(request.updates());
      }
    }

    for (var item : items) {
      try {
        if (item.targetCategoryId() == null || item.transactionId() == null) {
          failed++;
          errors.add("Fila inválida de actualización.");
          continue;
        }
        var targetCategory = getCategoryForProfile(profileId, item.targetCategoryId());
        var transaction = repository.findByIdAndProfileId(item.transactionId(), profileId)
                .orElse(null);

        if (transaction == null) {
          failed++;
          errors.add("Movimiento no encontrado o no pertenece al perfil: " + item.transactionId());
          continue;
        }

        if (Objects.equals(transaction.getCategoryId(), targetCategory.getId())) {
          skipped++;
          warnings.add("Movimiento omitido porque ya tiene la categoría destino: " + item.transactionId());
          continue;
        }

        if (!isMovementCategoryCompatible(transaction.getMovementType(), targetCategory.getType())) {
          failed++;
          errors.add("Movimiento incompatible con la categoría destino: " + item.transactionId());
          continue;
        }

        transaction.setCategoryId(targetCategory.getId());
        repository.save(transaction);

        updated++;
        updatedIds.add(transaction.getId());
      } catch (Exception ex) {
        failed++;
        errors.add("Error actualizando movimiento " + item.transactionId() + ": " + ex.getMessage());
      }
    }

    return new BulkRecategorizeApplyResponse(
            updated,
            skipped,
            failed,
            updatedIds,
            warnings,
            errors
    );
  }

  private List<MoneyTransaction> loadBulkBaseTransactions(
          UUID profileId,
          BulkRecategorizePreviewRequest request,
          LocalDate from,
          LocalDate to
  ) {
    if (request.accountId() != null && request.fromCategoryId() != null && !Boolean.TRUE.equals(request.onlyWithoutCategory())) {
      return repository.findByProfileIdAndAccountIdAndCategoryIdAndRealDateBetween(
              profileId,
              request.accountId(),
              request.fromCategoryId(),
              from,
              to
      );
    }

    if (request.accountId() != null) {
      return repository.findByProfileIdAndAccountIdAndRealDateBetween(
              profileId,
              request.accountId(),
              from,
              to
      );
    }

    if (request.fromCategoryId() != null && !Boolean.TRUE.equals(request.onlyWithoutCategory())) {
      return repository.findByProfileIdAndCategoryIdAndRealDateBetween(
              profileId,
              request.fromCategoryId(),
              from,
              to
      );
    }

    return repository.findByProfileIdAndRealDateBetween(profileId, from, to);
  }

  private boolean matchesBulkFilters(
          MoneyTransaction transaction,
          BulkRecategorizePreviewRequest request
  ) {
    if (request.movementType() != null
            && transaction.getMovementType() != request.movementType()) {
      return false;
    }
    if (Boolean.TRUE.equals(request.onlyWithoutCategory()) && transaction.getCategoryId() != null) {
      return false;
    }

    if (Boolean.TRUE.equals(request.onlyImported())
            && transaction.getOrigin() != MoneyTransaction.Origin.IMPORT) {
      return false;
    }

    if (request.exactAmount() != null
            && transaction.getAmount().compareTo(request.exactAmount()) != 0) {
      return false;
    }

    if (request.minAmount() != null
            && transaction.getAmount().compareTo(request.minAmount()) < 0) {
      return false;
    }

    if (request.maxAmount() != null
            && transaction.getAmount().compareTo(request.maxAmount()) > 0) {
      return false;
    }

    if (request.descriptionContains() != null && !request.descriptionContains().isBlank()) {
      var transactionDescription = normalizeText(transaction.getDescription());
      var expectedDescription = normalizeText(request.descriptionContains());

      return transactionDescription.contains(expectedDescription);
    }

    return true;
  }

  private HashMap<String, Integer> buildAmbiguityCountByKey(List<MoneyTransaction> transactions) {
    var countByKey = new HashMap<String, Integer>();

    for (var transaction : transactions) {
      var key = ambiguityKey(transaction);
      countByKey.put(key, countByKey.getOrDefault(key, 0) + 1);
    }

    return countByKey;
  }

  private String ambiguityKey(MoneyTransaction transaction) {
    return transaction.getAccountId()
            + "|"
            + transaction.getRealDate()
            + "|"
            + transaction.getAmount();
  }

  private BulkRecategorizeCandidate toBulkCandidate(
          MoneyTransaction transaction,
          UUID targetCategoryId,
          String targetCategoryName,
          String previewStatus,
          String warning
  ) {
    String currentCategoryName = transaction.getCategoryId() == null
            ? null
            : categoryRepository.findById(transaction.getCategoryId()).map(Category::getName).orElse(null);
    return new BulkRecategorizeCandidate(
            transaction.getId(),
            transaction.getAccountId(),
            transaction.getCategoryId(),
            currentCategoryName,
            targetCategoryId,
            targetCategoryName,
            transaction.getMovementType(),
            transaction.getRealDate(),
            transaction.getBudgetDate(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.getOrigin(),
            transaction.getStatus(),
            previewStatus,
            warning
    );
  }

  private BulkRecategorizePreviewResponse emptyBulkPreview(
          UUID profileId,
          UUID targetCategoryId,
          List<String> errors
  ) {
    return new BulkRecategorizePreviewResponse(
            profileId,
            targetCategoryId,
            0,
            0,
            0,
            0,
            List.of(),
            List.of(),
            errors
    );
  }

  private Category getCategoryForProfile(UUID profileId, UUID categoryId) {
    if (categoryId == null) {
      throw new BadRequestException("Category is required");
    }

    var category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("Category not found"));

    if (category.getProfileId() != null && !Objects.equals(category.getProfileId(), profileId)) {
      throw new ForbiddenException("Category does not belong to profile");
    }

    if (!Boolean.TRUE.equals(category.getActive())) {
      throw new BadRequestException("Category is not active");
    }

    return category;
  }

  private void validate(
          UUID profileId,
          UUID accountId,
          UUID categoryId,
          MoneyTransaction.MovementType movementType,
          UUID userId
  ) {
    ensureProfile(profileId, userId);

    if (!accountRepository.existsByIdAndProfileId(accountId, profileId)) {
      throw new BadRequestException("Account does not belong to profile");
    }

    if (categoryId == null) {
      return;
    }

    var category = getCategoryForProfile(profileId, categoryId);

    validateMovementCategory(movementType, category.getType());
  }

  private void validateMovementCategory(
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType
  ) {
    if (!isMovementCategoryCompatible(movementType, categoryType)) {
      if (movementType == MoneyTransaction.MovementType.INCOME) {
        throw new BadRequestException("Movement/category mismatch: INCOME requiere categoría INCOME");
      }

      if (movementType == MoneyTransaction.MovementType.SAVING) {
        throw new BadRequestException("Movement/category mismatch: SAVING requiere categoría SAVING/INVESTMENT");
      }

      if (movementType == MoneyTransaction.MovementType.EXPENSE) {
        throw new BadRequestException("Movement/category mismatch: EXPENSE no permite categoría INCOME");
      }

      throw new BadRequestException("Movement/category mismatch");
    }
  }

  private boolean isMovementCategoryCompatible(
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

  private void ensureProfile(UUID profileId, UUID userId) {
    profileRepository.findByIdAndUserId(profileId, userId)
            .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
  }

  private TransactionResponse toResponse(MoneyTransaction transaction) {
    return new TransactionResponse(
            transaction.getId(),
            transaction.getProfileId(),
            transaction.getAccountId(),
            transaction.getCategoryId(),
            transaction.getMovementType(),
            transaction.getRealDate(),
            transaction.getBudgetDate(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getDescription(),
            transaction.getOrigin(),
            transaction.getStatus(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
    );
  }

  private String normalizeText(String value) {
    if (value == null) {
      return "";
    }

    var clean = value
            .replace('\u00A0', ' ')
            .trim();

    clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

    return clean
            .toUpperCase()
            .replaceAll("\\s+", " ")
            .trim();
  }

  private String normalizeTargetMode(String targetMode) {
    return targetMode == null ? "MANUAL" : targetMode;
  }

  private boolean isManualMode(String targetMode) {
    return !"AUTO_BY_IMPORT_RULES".equals(targetMode);
  }

  private boolean isAutoMode(String targetMode) {
    return "AUTO_BY_IMPORT_RULES".equals(targetMode);
  }

  public record TransactionMetadata(
          String source,
          String sourceOperationId,
          String sourceHash,
          MoneyTransaction.PaymentChannel paymentChannel,
          String counterparty,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String classificationReason,
          UUID importBatchId,
          UUID internalTransferGroupId
  ) {
  }
}
