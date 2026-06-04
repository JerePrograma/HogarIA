package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.entity.ImportRowStatus;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.service.TransactionService;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportCommitService {

    private final FinancialProfileRepository profileRepository;
    private final AccountRepository accountRepository;
    private final TransactionService txService;
    private final TransactionImportBatchStore batchStore;
    private final TransactionImportDuplicateDetector duplicateDetector;
    private final TransactionImportCategoryResolver categoryResolver;

    public TransactionImportCommitService(
            FinancialProfileRepository profileRepository,
            AccountRepository accountRepository,
            TransactionService txService,
            TransactionImportBatchStore batchStore,
            TransactionImportDuplicateDetector duplicateDetector,
            TransactionImportCategoryResolver categoryResolver
    ) {
        this.profileRepository = profileRepository;
        this.accountRepository = accountRepository;
        this.txService = txService;
        this.batchStore = batchStore;
        this.duplicateDetector = duplicateDetector;
        this.categoryResolver = categoryResolver;
    }

    public TransactionImportCommitResponse commit(
            UUID userId,
            UUID profileId,
            UUID batchId,
            TransactionImportCommitRequest request
    ) {
        ensureProfileBelongsToUser(profileId, userId);

        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new BadRequestException("No hay filas para importar.");
        }

        var batch = batchStore.getBatchOrThrow(batchId);
        var loadedRows = batchStore.loadRowSnapshots(batchId);

        if (loadedRows.isEmpty()) {
            throw new BadRequestException("El batch no tiene filas importables.");
        }

        var rowsByNumber = new HashMap<Integer, TransactionImportBatchStore.LoadedPreviewRow>();

        for (var row : loadedRows) {
            rowsByNumber.put(row.previewRow().rowNumber(), row);
        }

        var accumulator = new TransactionImportCommitAccumulator();

        for (var commitRow : request.rows()) {
            var result = commitSingleRow(
                    userId,
                    profileId,
                    batchId,
                    batch.getAccountId(),
                    rowsByNumber,
                    commitRow,
                    request,
                    accumulator
            );

            accumulator.add(result);
        }

        batchStore.completeBatch(
                batchId,
                accumulator.created(),
                accumulator.skipped(),
                accumulator.duplicates(),
                accumulator.failed()
        );

        return accumulator.toResponse();
    }

    private TransactionImportCommitResult commitSingleRow(
            UUID userId,
            UUID profileId,
            UUID batchId,
            UUID batchAccountId,
            Map<Integer, TransactionImportBatchStore.LoadedPreviewRow> rowsByNumber,
            TransactionImportCommitRow commitRow,
            TransactionImportCommitRequest request,
            TransactionImportCommitAccumulator accumulator
    ) {
        var loadedRow = rowsByNumber.get(commitRow.rowNumber());
        var previewRow = loadedRow == null ? null : loadedRow.previewRow();

        if (previewRow == null) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": no existe en el batch.");
            return TransactionImportCommitResult.failed();
        }

        if (isSkipped(commitRow, previewRow)) {
            batchStore.markImportRow(
                    loadedRow.entity(),
                    ImportRowStatus.SKIPPED,
                    ImportTextSupport.firstNonBlank(previewRow.skipReason(), "Omitida por regla de preview.")
            );

            return TransactionImportCommitResult.skipped();
        }

        if (isError(commitRow, previewRow)) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": fila inválida en preview.");
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, "Fila inválida en preview.");
            return TransactionImportCommitResult.failed();
        }

        if (isBlockingDuplicate(commitRow.status()) || isBlockingDuplicate(previewRow.status())) {
            if (request.skipDuplicates()) {
                batchStore.markImportRow(
                        loadedRow.entity(),
                        ImportRowStatus.SKIPPED,
                        "Omitida por duplicado detectado."
                );

                return TransactionImportCommitResult.duplicate();
            }

            accumulator.warning("Fila " + commitRow.rowNumber() + ": marcada como duplicada, se intenta importar.");
        }

        if (isReviewRisk(commitRow.status()) || isReviewRisk(previewRow.status())) {
            accumulator.warning(
                    "Fila " + commitRow.rowNumber()
                            + ": marcada para revisión por transferencia interna/duplicado cruzado. No se omite automáticamente."
            );
        }

        var amount = commitRow.amount() != null
                ? commitRow.amount()
                : previewRow.amount();

        if (amount == null || amount.signum() <= 0) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": monto inválido.");
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, "Monto inválido.");
            return TransactionImportCommitResult.failed();
        }

        var accountId = commitRow.accountId() != null
                ? commitRow.accountId()
                : batchAccountId;

        if (accountId == null) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": falta accountId.");
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, "Falta accountId.");
            return TransactionImportCommitResult.failed();
        }

        try {
            ensureAccountBelongsToProfile(accountId, profileId);
        } catch (Exception ex) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": la cuenta no pertenece al perfil.");
            batchStore.markImportRow(
                    loadedRow.entity(),
                    ImportRowStatus.ERROR,
                    "La cuenta no pertenece al perfil."
            );
            return TransactionImportCommitResult.failed();
        }

        var movementType = resolveMovementType(commitRow, previewRow);

        UUID categoryId;

        try {
            categoryId = categoryResolver.resolveCategoryId(
                    profileId,
                    commitRow,
                    previewRow,
                    request,
                    movementType
            );
        } catch (Exception ex) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": " + ex.getMessage());
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, ex.getMessage());
            return TransactionImportCommitResult.failed();
        }

        if (!validateCategoryDecision(profileId, loadedRow, previewRow, commitRow, movementType, categoryId, accumulator)) {
            return TransactionImportCommitResult.failed();
        }

        var description = ImportTextSupport.firstNonBlank(
                commitRow.description(),
                previewRow.rawDescription(),
                previewRow.normalizedDescription(),
                "Movimiento importado"
        );

        var match = duplicateDetector.findImportMatch(profileId, accountId, previewRow);

        if (match.found()) {
            if (match.type().isDuplicate() && request.skipDuplicates()) {
                accumulator.warning("Fila " + commitRow.rowNumber() + ": omitida por duplicado.");
                batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.SKIPPED, "Omitida por duplicado.");
                return TransactionImportCommitResult.duplicate();
            }

            accumulator.warning(
                    "Fila " + commitRow.rowNumber()
                            + ": " + ImportTextSupport.firstNonBlank(match.reason(), "posible coincidencia detectada.")
            );
        }

        try {
            var paymentChannel = previewRow.paymentChannel() == null
                    ? categoryResolver.inferPaymentChannel(previewRow.source(), description)
                    : previewRow.paymentChannel();

            var classificationStatus = categoryResolver.inferClassificationStatus(previewRow, categoryId);
            var classificationReason = categoryResolver.inferClassificationReason(previewRow);
            var sourceName = previewRow.source() == null ? null : previewRow.source().name();

            var response = txService.create(
                    new TransactionCreateRequest(
                            profileId,
                            accountId,
                            categoryId,
                            movementType,
                            previewRow.realDate(),
                            previewRow.budgetDate(),
                            previewRow.operationDateTime(),
                            amount,
                            ImportTextSupport.firstNonBlank(previewRow.currency(), ImportTextSupport.DEFAULT_CURRENCY)
                                    .toUpperCase(Locale.ROOT),
                            description,
                            MoneyTransaction.Origin.IMPORT,
                            categoryResolver.importStatusFor(previewRow, categoryId),
                            sourceName,
                            previewRow.sourceOperationId(),
                            previewRow.sourceHash(),
                            paymentChannel,
                            previewRow.counterparty(),
                            classificationStatus,
                            classificationReason,
                            previewRow.classificationExplanationJson(),
                            batchId,
                            null
                    ),
                    userId,
                    new TransactionService.TransactionMetadata(
                            sourceName,
                            previewRow.sourceOperationId(),
                            previewRow.sourceHash(),
                            paymentChannel,
                            previewRow.counterparty(),
                            classificationStatus,
                            classificationReason,
                            previewRow.classificationExplanationJson(),
                            previewRow.balanceImpact(),
                            batchId,
                            null
                    )
            );

            batchStore.saveImportReference(
                    profileId,
                    accountId,
                    batchId,
                    loadedRow.entity(),
                    previewRow,
                    response.id()
            );

            batchStore.saveClassificationAudit(
                    loadedRow.entity(),
                    previewRow,
                    response.id(),
                    categoryId
            );

            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.IMPORTED, null);

            return TransactionImportCommitResult.created(response.id());
        } catch (Exception ex) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": " + ex.getMessage());
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, ex.getMessage());
            return TransactionImportCommitResult.failed();
        }
    }

    private boolean validateCategoryDecision(
            UUID profileId,
            TransactionImportBatchStore.LoadedPreviewRow loadedRow,
            TransactionImportPreviewRow previewRow,
            TransactionImportCommitRow commitRow,
            MoneyTransaction.MovementType movementType,
            UUID categoryId,
            TransactionImportCommitAccumulator accumulator
    ) {
        if (categoryId == null) {
            if (!categoryResolver.canImportWithoutCategory(previewRow)) {
                accumulator.error("Fila " + commitRow.rowNumber() + ": falta categoría.");
                batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, "Falta categoría.");
                return false;
            }

            return true;
        }

        var category = categoryResolver.findCategory(categoryId);

        if (category == null) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": categoría inexistente.");
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, "Categoría inexistente.");
            return false;
        }

        try {
            categoryResolver.ensureCategoryBelongsToProfileOrIsGlobal(profileId, categoryId);
        } catch (Exception ex) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": " + ex.getMessage());
            batchStore.markImportRow(loadedRow.entity(), ImportRowStatus.ERROR, ex.getMessage());
            return false;
        }

        if (!categoryResolver.isMovementCategoryCompatible(movementType, category.getType())) {
            accumulator.error("Fila " + commitRow.rowNumber() + ": tipo de movimiento/categoría incompatible.");
            batchStore.markImportRow(
                    loadedRow.entity(),
                    ImportRowStatus.ERROR,
                    "Tipo de movimiento/categoría incompatible."
            );
            return false;
        }

        return true;
    }

    private MoneyTransaction.MovementType resolveMovementType(
            TransactionImportCommitRow commitRow,
            TransactionImportPreviewRow previewRow
    ) {
        if (commitRow.movementType() != null) {
            return commitRow.movementType();
        }

        if (previewRow.movementType() != null) {
            return previewRow.movementType();
        }

        var signedAmount = previewRow.rawSignedAmount();

        if (signedAmount != null && signedAmount.signum() > 0) {
            return MoneyTransaction.MovementType.INCOME;
        }

        return MoneyTransaction.MovementType.EXPENSE;
    }

    private boolean isSkipped(
            TransactionImportCommitRow commitRow,
            TransactionImportPreviewRow previewRow
    ) {
        return commitRow.status() == RowStatus.SKIPPED
                || previewRow.status() == RowStatus.SKIPPED;
    }

    private boolean isError(
            TransactionImportCommitRow commitRow,
            TransactionImportPreviewRow previewRow
    ) {
        return commitRow.status() == RowStatus.ERROR
                || previewRow.status() == RowStatus.ERROR;
    }

    private boolean isBlockingDuplicate(RowStatus status) {
        return status == RowStatus.DUPLICATE
                || status == RowStatus.DUPLICATE_EXACT;
    }

    private boolean isReviewRisk(RowStatus status) {
        return status == RowStatus.POSSIBLE_INTERNAL_TRANSFER
                || status == RowStatus.INTERNAL_TRANSFER_MATCHED
                || status == RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE;
    }

    private void ensureProfileBelongsToUser(UUID profileId, UUID userId) {
        profileRepository
                .findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
    }

    private void ensureAccountBelongsToProfile(UUID accountId, UUID profileId) {
        if (accountId == null || !accountRepository.existsByIdAndProfileId(accountId, profileId)) {
            throw new BadRequestException("Account does not belong to profile");
        }
    }
}