package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.exception.BadRequestException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.ExcelImportBatchRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionImportPreviewService {

    private static final String DEFAULT_CURRENCY = "ARS";

    private final FinancialProfileRepository profileRepository;
    private final AccountRepository accountRepository;
    private final ExcelImportBatchRepository batchRepository;
    private final TransactionImportParserRouter parserRouter;
    private final TransactionImportDuplicateDetector duplicateDetector;
    private final TransactionImportBatchStore batchStore;
    private final TransactionImportSummaryFactory summaryFactory;
    private final ImportInternalTransferMatchingService internalTransferMatchingService;

    public TransactionImportPreviewService(
            FinancialProfileRepository profileRepository,
            AccountRepository accountRepository,
            ExcelImportBatchRepository batchRepository,
            TransactionImportParserRouter parserRouter,
            TransactionImportDuplicateDetector duplicateDetector,
            TransactionImportBatchStore batchStore,
            TransactionImportSummaryFactory summaryFactory,
            ImportInternalTransferMatchingService internalTransferMatchingService
    ) {
        this.profileRepository = profileRepository;
        this.accountRepository = accountRepository;
        this.batchRepository = batchRepository;
        this.parserRouter = parserRouter;
        this.duplicateDetector = duplicateDetector;
        this.batchStore = batchStore;
        this.summaryFactory = summaryFactory;
        this.internalTransferMatchingService = internalTransferMatchingService;
    }

    public TransactionImportPreviewResponse preview(
            UUID userId,
            UUID profileId,
            UUID accountId,
            TransactionImportSource source,
            MultipartFile file,
            Integer year,
            Integer month
    ) {
        ensureProfileBelongsToUser(profileId, userId);
        ensureAccountBelongsToProfile(accountId, profileId);

        var requestedSource = source == null ? TransactionImportSource.AUTO : source;
        var rows = parserRouter.parse(requestedSource, file, profileId, accountId, year, month);
        var resolvedRows = duplicateDetector.applyDuplicateStatus(profileId, accountId, rows);

        var actualSource = resolveActualSource(requestedSource, resolvedRows);
        var detectedFormat = detectedFormat(resolvedRows);

        var batch = batchStore.createPreviewBatch(
                profileId,
                accountId,
                actualSource,
                file.getOriginalFilename(),
                DEFAULT_CURRENCY,
                year,
                month,
                Map.of(
                        "detectedFormat", detectedFormat == null ? "" : detectedFormat,
                        "requestedSource", requestedSource.name(),
                        "actualSource", actualSource.name()
                )
        );

        batchStore.savePreviewRows(batch.getId(), actualSource, resolvedRows);

        var matchedRows = internalTransferMatchingService.applyToBatch(profileId, batch.getId());
        return summaryFactory.summarize(batch.getId(), actualSource, accountId, matchedRows);
    }

    public TransactionImportPreviewResponse getBatch(UUID userId, UUID profileId, UUID batchId) {
        ensureProfileBelongsToUser(profileId, userId);

        var batch = batchRepository
                .findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));

        if (!batch.getProfileId().equals(profileId)) {
            throw new ForbiddenException("Batch does not belong to profile");
        }

        var rows = batchStore.loadRows(batchId);

        if (rows.isEmpty()) {
            throw new NotFoundException("Batch has no readable rows");
        }

        var source = batch.getSource() == null
                ? rows.get(0).source()
                : TransactionImportSource.valueOf(batch.getSource());

        return summaryFactory.summarize(batchId, source, batch.getAccountId(), rows);
    }

    private TransactionImportSource resolveActualSource(
            TransactionImportSource requestedSource,
            java.util.List<TransactionImportPreviewRow> rows
    ) {
        if (rows.isEmpty() || rows.get(0).source() == null || rows.get(0).source() == TransactionImportSource.AUTO) {
            return requestedSource;
        }

        return rows.get(0).source();
    }

    private String detectedFormat(java.util.List<TransactionImportPreviewRow> rows) {
        return rows
                .stream()
                .map(TransactionImportPreviewRow::detectedFormat)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
