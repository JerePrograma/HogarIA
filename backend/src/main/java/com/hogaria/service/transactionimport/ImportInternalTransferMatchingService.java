package com.hogaria.service.transactionimport;

import com.hogaria.entity.ImportCounterpartyAlias;
import com.hogaria.entity.ImportInternalTransferMatch;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.ImportCounterpartyAliasRepository;
import com.hogaria.repository.ImportInternalTransferMatchRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.TransactionImportReferenceRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportInternalTransferMatchingService {

    private final ImportInternalTransferMatcher matcher;
    private final ImportCounterpartyAliasRepository aliasRepository;
    private final ImportInternalTransferMatchRepository matchRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final TransactionImportReferenceRepository referenceRepository;
    private final TransactionImportBatchStore batchStore;

    public ImportInternalTransferMatchingService(
            ImportInternalTransferMatcher matcher,
            ImportCounterpartyAliasRepository aliasRepository,
            ImportInternalTransferMatchRepository matchRepository,
            MoneyTransactionRepository transactionRepository,
            TransactionImportReferenceRepository referenceRepository,
            TransactionImportBatchStore batchStore
    ) {
        this.matcher = matcher;
        this.aliasRepository = aliasRepository;
        this.matchRepository = matchRepository;
        this.transactionRepository = transactionRepository;
        this.referenceRepository = referenceRepository;
        this.batchStore = batchStore;
    }

    @Transactional
    public java.util.List<com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow> applyToBatch(
            UUID profileId,
            UUID batchId
    ) {
        var snapshots = batchStore.loadProfileRowSnapshots(profileId);
        var ownIds = loadOwnIds(profileId);
        var result = matcher.match(snapshots, ownIds);

        for (var snapshot : result.rows()) {
            batchStore.updatePreviewRow(snapshot.importRowId(), snapshot.row());
        }

        for (var pair : result.matches()) {
            if (pair.debitImportRowId() == null || pair.creditImportRowId() == null) {
                continue;
            }
            var entity = matchRepository
                    .findByDebitImportRowIdAndCreditImportRowId(pair.debitImportRowId(), pair.creditImportRowId())
                    .orElseGet(() -> ImportInternalTransferMatch.builder()
                            .profileId(profileId)
                            .debitImportRowId(pair.debitImportRowId())
                            .creditImportRowId(pair.creditImportRowId())
                            .build());
            entity.setAmount(pair.amount());
            entity.setOperationDate(pair.operationDate());
            entity.setConfidence(pair.confidence());
            entity.setReason(pair.reason());
            referenceRepository.findByProfileIdAndImportRowId(profileId, pair.debitImportRowId())
                    .ifPresent(reference -> entity.setDebitTransactionId(reference.getTransactionId()));
            referenceRepository.findByProfileIdAndImportRowId(profileId, pair.creditImportRowId())
                    .ifPresent(reference -> entity.setCreditTransactionId(reference.getTransactionId()));
            confirmIfComplete(profileId, matchRepository.save(entity));
        }

        return result.rows().stream()
                .filter(snapshot -> batchId.equals(snapshot.batchId()))
                .map(ImportInternalTransferMatcher.ImportRowSnapshot::row)
                .sorted(java.util.Comparator.comparing(
                        com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow::rowNumber,
                        java.util.Comparator.nullsLast(Integer::compareTo)
                ))
                .toList();
    }

    @Transactional
    public void attachTransaction(UUID profileId, UUID importRowId, UUID transactionId) {
        for (var match : matchRepository.findByProfileIdAndImportRowId(profileId, importRowId)) {
            if (importRowId.equals(match.getDebitImportRowId())) {
                match.setDebitTransactionId(transactionId);
            }
            if (importRowId.equals(match.getCreditImportRowId())) {
                match.setCreditTransactionId(transactionId);
            }
            confirmIfComplete(profileId, matchRepository.save(match));
        }
    }

    private Set<String> loadOwnIds(UUID profileId) {
        return aliasRepository.findByProfileIdAndActiveTrue(profileId).stream()
                .filter(alias -> alias.getAliasType() == ImportCounterpartyAlias.AliasType.OWN_ID)
                .map(ImportCounterpartyAlias::getIdentifier)
                .collect(Collectors.toSet());
    }

    private void markConfirmed(UUID profileId, UUID transactionId, UUID groupId) {
        transactionRepository.findByIdAndProfileId(transactionId, profileId).ifPresent(transaction -> {
            transaction.setInternalTransferGroupId(groupId);
            transaction.setMovementType(MoneyTransaction.MovementType.TRANSFER);
            transaction.setBalanceImpact(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER);
            transaction.setClassificationStatus(MoneyTransaction.ClassificationStatus.TECHNICAL);
            transaction.setClassificationReason("INTERNAL_TRANSFER_MATCHED");
            transactionRepository.save(transaction);
        });
    }

    private void confirmIfComplete(UUID profileId, ImportInternalTransferMatch match) {
        if (match.getConfidence().compareTo(new java.math.BigDecimal("0.90")) >= 0
                && match.getDebitTransactionId() != null
                && match.getCreditTransactionId() != null) {
            markConfirmed(profileId, match.getDebitTransactionId(), match.getId());
            markConfirmed(profileId, match.getCreditTransactionId(), match.getId());
        }
    }
}
