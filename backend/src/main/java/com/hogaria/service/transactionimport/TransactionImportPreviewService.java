package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
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
    private final ExcelImportBatchRepository batchRepository;
    private final TransactionImportParserRouter parserRouter;
    private final TransactionImportDuplicateDetector duplicateDetector;
    private final TransactionImportBatchStore batchStore;
    private final TransactionImportSummaryFactory summaryFactory;

    public TransactionImportPreviewService(
            FinancialProfileRepository profileRepository,
            ExcelImportBatchRepository batchRepository,
            TransactionImportParserRouter parserRouter,
            TransactionImportDuplicateDetector duplicateDetector,
            TransactionImportBatchStore batchStore,
            TransactionImportSummaryFactory summaryFactory
    ) {
        this.profileRepository = profileRepository;
        this.batchRepository = batchRepository;
        this.parserRouter = parserRouter;
        this.duplicateDetector = duplicateDetector;
        this.batchStore = batchStore;
        this.summaryFactory = summaryFactory;
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

        return summaryFactory.summarize(batch.getId(), actualSource, accountId, resolvedRows);
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

        return summaryFactory.summarize(batchId, rows.get(0).source(), null, rows);
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
}