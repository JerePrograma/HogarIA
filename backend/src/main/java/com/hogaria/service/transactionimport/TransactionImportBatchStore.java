package com.hogaria.service.transactionimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ExcelImportBatch;
import com.hogaria.entity.ExcelImportRow;
import com.hogaria.entity.ImportBatchStatus;
import com.hogaria.entity.ImportRowStatus;
import com.hogaria.entity.TransactionClassificationAudit;
import com.hogaria.entity.TransactionImportReference;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.ExcelImportBatchRepository;
import com.hogaria.repository.ExcelImportRowRepository;
import com.hogaria.repository.TransactionClassificationAuditRepository;
import com.hogaria.repository.TransactionImportReferenceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportBatchStore {

    private final ExcelImportBatchRepository batchRepository;
    private final ExcelImportRowRepository rowRepository;
    private final TransactionImportReferenceRepository referenceRepository;
    private final TransactionClassificationAuditRepository classificationAuditRepository;
    private final ObjectMapper objectMapper;

    public TransactionImportBatchStore(
            ExcelImportBatchRepository batchRepository,
            ExcelImportRowRepository rowRepository,
            TransactionImportReferenceRepository referenceRepository,
            TransactionClassificationAuditRepository classificationAuditRepository,
            ObjectMapper objectMapper
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.referenceRepository = referenceRepository;
        this.classificationAuditRepository = classificationAuditRepository;
        this.objectMapper = objectMapper;
    }

    public ExcelImportBatch getBatchOrThrow(UUID batchId) {
        return batchRepository
                .findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));
    }

    public ExcelImportBatch createPreviewBatch(
            UUID profileId,
            UUID accountId,
            TransactionImportSource actualSource,
            String originalFileName,
            String currency,
            Integer year,
            Integer month,
            Map<String, Object> summary
    ) {
        return batchRepository.save(
                ExcelImportBatch.builder()
                        .profileId(profileId)
                        .accountId(accountId)
                        .source(actualSource == TransactionImportSource.AUTO ? null : actualSource.name())
                        .originalFileName(originalFileName)
                        .currency(currency)
                        .status(ImportBatchStatus.PREVIEWED)
                        .year(year)
                        .month(month)
                        .summaryJson(writeJson(summary))
                        .warningsJson("[]")
                        .errorsJson("[]")
                        .build()
        );
    }

    public void savePreviewRows(
            UUID batchId,
            TransactionImportSource source,
            List<TransactionImportPreviewRow> rows
    ) {
        for (var row : rows) {
            savePreviewRow(batchId, source, row);
        }
    }

    public List<TransactionImportPreviewRow> loadRows(UUID batchId) {
        return loadRowSnapshots(batchId)
                .stream()
                .map(LoadedPreviewRow::previewRow)
                .toList();
    }

    public List<LoadedPreviewRow> loadRowSnapshots(UUID batchId) {
        return rowRepository
                .findByBatchIdOrderByRowNumber(batchId)
                .stream()
                .map(this::toLoadedPreviewRow)
                .toList();
    }

    public List<ImportInternalTransferMatcher.ImportRowSnapshot> loadProfileRowSnapshots(UUID profileId) {
        return rowRepository.findByProfileIdOrderByBatchAndRow(profileId)
                .stream()
                .map(this::toLoadedPreviewRow)
                .map(loaded -> new ImportInternalTransferMatcher.ImportRowSnapshot(
                        loaded.entity().getId(),
                        loaded.entity().getBatchId(),
                        loaded.previewRow()
                ))
                .toList();
    }

    public void updatePreviewRow(UUID importRowId, TransactionImportPreviewRow previewRow) {
        if (importRowId == null || previewRow == null) {
            return;
        }
        rowRepository.findById(importRowId).ifPresent(entity -> {
            applyPreviewRow(entity, previewRow);
            rowRepository.save(entity);
        });
    }

    public void markImportRow(ExcelImportRow row, ImportRowStatus status, String message) {
        if (row == null) {
            return;
        }

        row.setStatus(status);
        row.setErrorMessage(message == null ? null : ImportTextSupport.truncate(message, 1000));
        rowRepository.save(row);
    }

    public void completeBatch(
            UUID batchId,
            int created,
            int skipped,
            int duplicates,
            int failed
    ) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            batch.setStatus(failed > 0 && created == 0
                    ? ImportBatchStatus.FAILED
                    : ImportBatchStatus.COMPLETED
            );

            batch.setSummaryJson(writeJson(Map.of(
                    "created", created,
                    "skipped", skipped,
                    "duplicates", duplicates,
                    "failed", failed
            )));

            batchRepository.save(batch);
        });
    }

    public void saveImportReference(
            UUID profileId,
            UUID accountId,
            UUID batchId,
            ExcelImportRow importRow,
            TransactionImportPreviewRow previewRow,
            UUID transactionId
    ) {
        referenceRepository.save(
                TransactionImportReference.builder()
                        .transactionId(transactionId)
                        .profileId(profileId)
                        .accountId(accountId)
                        .importBatchId(batchId)
                        .importRowId(importRow == null ? null : importRow.getId())
                        .importSource(previewRow.source() == null ? null : previewRow.source().name())
                        .sourceOperationId(previewRow.sourceOperationId())
                        .sourceHash(previewRow.sourceHash())
                        .externalSequence(previewRow.externalSequence())
                        .rawDescription(ImportTextSupport.truncate(previewRow.rawDescription(), 255))
                        .normalizedDescription(ImportTextSupport.truncate(previewRow.normalizedDescription(), 500))
                        .extendedDescription(ImportTextSupport.truncate(previewRow.extendedDescription(), 500))
                        .merchantName(ImportTextSupport.truncate(previewRow.merchantName(), 255))
                        .counterpartyName(ImportTextSupport.truncate(previewRow.counterparty(), 255))
                        .counterpartyDocumentHash(previewRow.counterpartyDocumentHash())
                        .paymentChannel(previewRow.paymentChannel())
                        .classificationStatus(previewRow.classificationStatus())
                        .classificationReason(ImportTextSupport.truncate(previewRow.classificationReason(), 255))
                        .classificationExplanationJson(previewRow.classificationExplanationJson())
                        .rawPayload(safeJsonPayload(ImportTextSupport.firstNonBlank(
                                previewRow.rawJson(),
                                previewRow.rawPayload()
                        )))
                        .build()
        );
    }

    public void saveClassificationAudit(
            ExcelImportRow importRow,
            TransactionImportPreviewRow previewRow,
            UUID transactionId,
            UUID categoryId
    ) {
        if (previewRow == null) {
            return;
        }

        classificationAuditRepository.save(
                TransactionClassificationAudit.builder()
                        .transactionId(transactionId)
                        .importRowId(importRow == null ? null : importRow.getId())
                        .ruleId(null)
                        .reasonCode(ImportTextSupport.firstNonBlank(
                                previewRow.classificationReason(),
                                "NO_IMPORT_RULE"
                        ))
                        .matchedField(ImportTextSupport.truncate(previewRow.classificationMatchedField(), 80))
                        .matchedValue(ImportTextSupport.truncate(previewRow.classificationMatchedValue(), 500))
                        .suggestedCategoryId(categoryId)
                        .confidence(confidenceScore(previewRow.confidence()))
                        .build()
        );
    }

    private void savePreviewRow(
            UUID batchId,
            TransactionImportSource source,
            TransactionImportPreviewRow row
    ) {
        try {
            var entity = ExcelImportRow.builder()
                            .batchId(batchId)
                            .sheetName(ImportTextSupport.firstNonBlank(
                                    row.sheetName(),
                                    source == null ? null : source.name()
                            ))
                            .build();
            applyPreviewRow(entity, row);
            rowRepository.save(entity);
        } catch (Exception ex) {
            throw new BadRequestException(
                    "No se pudo guardar la fila de preview " + row.rowNumber() + ": " + ex.getMessage()
            );
        }
    }

    private LoadedPreviewRow toLoadedPreviewRow(ExcelImportRow entity) {
        try {
            return new LoadedPreviewRow(
                    objectMapper.readValue(entity.getRawJson(), TransactionImportPreviewRow.class),
                    entity
            );
        } catch (Exception ignored) {
            throw new BadRequestException(
                    "No se pudo leer la fila persistida " + entity.getRowNumber()
                            + " del batch " + entity.getBatchId() + "."
            );
        }
    }

    private void applyPreviewRow(ExcelImportRow entity, TransactionImportPreviewRow row) {
        try {
            entity.setRowNumber(row.rowNumber());
            entity.setConcept(ImportTextSupport.truncate(row.rawDescription(), 255));
            entity.setMonth(row.budgetDate() == null ? null : row.budgetDate().getMonthValue());
            entity.setRealDate(row.realDate());
            entity.setBudgetDate(row.budgetDate());
            entity.setAmount(row.amount());
            entity.setSignedAmount(row.rawSignedAmount());
            entity.setAmountAbs(row.amount());
            entity.setMovementType(row.movementType());
            entity.setSource(row.source() == null ? null : row.source().name());
            entity.setSourceOperationId(row.sourceOperationId());
            entity.setSourceHash(row.sourceHash());
            entity.setExternalSequence(row.externalSequence());
            entity.setOperationDateTime(row.operationDateTime());
            entity.setOperationDateTimePrecision(row.operationDateTimePrecision());
            entity.setRawDescription(ImportTextSupport.truncate(row.rawDescription(), 255));
            entity.setNormalizedDescription(ImportTextSupport.truncate(row.normalizedDescription(), 500));
            entity.setExtendedDescription(ImportTextSupport.truncate(row.extendedDescription(), 500));
            entity.setMerchantName(ImportTextSupport.truncate(row.merchantName(), 255));
            entity.setCounterpartyName(ImportTextSupport.truncate(row.counterparty(), 255));
            entity.setCounterparty(ImportTextSupport.truncate(row.counterparty(), 255));
            entity.setCounterpartyDocumentHash(row.counterpartyDocumentHash());
            entity.setPaymentChannel(row.paymentChannel());
            entity.setBalanceImpact(row.balanceImpact());
            entity.setClassificationStatus(row.classificationStatus());
            entity.setClassificationReason(ImportTextSupport.truncate(row.classificationReason(), 255));
            entity.setClassificationLayer(row.classificationLayer());
            entity.setClassificationMatchedField(ImportTextSupport.truncate(row.classificationMatchedField(), 80));
            entity.setClassificationMatchedValue(ImportTextSupport.truncate(row.classificationMatchedValue(), 500));
            entity.setClassificationExplanationJson(row.classificationExplanationJson());
            entity.setSuggestedCategoryId(row.suggestedCategoryId());
            entity.setSuggestedCategoryName(ImportTextSupport.truncate(row.suggestedCategoryName(), 255));
            entity.setTargetEntity(row.targetEntity());
            entity.setStatus(TransactionImportRowStatusMapper.toImportRowStatus(row.status()));
            entity.setErrorMessage(ImportTextSupport.truncate(row.skipReason(), 1000));
            entity.setWarning(ImportTextSupport.truncate(row.skipReason(), 1000));
            entity.setRawJson(objectMapper.writeValueAsString(row));
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo persistir snapshot de fila: " + ex.getMessage());
        }
    }

    private BigDecimal confidenceScore(Confidence confidence) {
        if (confidence == Confidence.HIGH) {
            return new BigDecimal("0.95");
        }

        if (confidence == Confidence.MEDIUM) {
            return new BigDecimal("0.70");
        }

        if (confidence == Confidence.LOW) {
            return new BigDecimal("0.35");
        }

        return BigDecimal.ZERO;
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String safeJsonPayload(String payload) {
        var clean = ImportTextSupport.firstNonBlank(payload);

        if (clean.isBlank()) {
            return "{}";
        }

        try {
            objectMapper.readTree(clean);
            return clean;
        } catch (Exception ignored) {
            return writeJson(Map.of("raw", clean));
        }
    }

    public record LoadedPreviewRow(
            TransactionImportPreviewRow previewRow,
            ExcelImportRow entity
    ) {
    }
}
