package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.hogaria.entity.ImportInternalTransferMatch;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.TransactionImportReference;
import com.hogaria.repository.ImportCounterpartyAliasRepository;
import com.hogaria.repository.ImportInternalTransferMatchRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.TransactionImportReferenceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportInternalTransferMatchingServiceTest {

    @Test
    void completedStrongMatchBackfillsPreviouslyImportedTransactionsWithinProfile() {
        var profileId = UUID.randomUUID();
        var batchId = UUID.randomUUID();
        var debitRowId = UUID.randomUUID();
        var creditRowId = UUID.randomUUID();
        var debitTransactionId = UUID.randomUUID();
        var creditTransactionId = UUID.randomUUID();
        var groupId = UUID.randomUUID();

        var matcher = mock(ImportInternalTransferMatcher.class);
        var aliasRepository = mock(ImportCounterpartyAliasRepository.class);
        var matchRepository = mock(ImportInternalTransferMatchRepository.class);
        var transactionRepository = mock(MoneyTransactionRepository.class);
        var referenceRepository = mock(TransactionImportReferenceRepository.class);
        var batchStore = mock(TransactionImportBatchStore.class);
        var service = new ImportInternalTransferMatchingService(
                matcher,
                aliasRepository,
                matchRepository,
                transactionRepository,
                referenceRepository,
                batchStore
        );

        var pair = new ImportInternalTransferMatcher.MatchedPair(
                debitRowId,
                creditRowId,
                new BigDecimal("10000.00"),
                LocalDate.of(2026, 5, 7),
                BigDecimal.ONE,
                "strong",
                true
        );
        when(batchStore.loadProfileRowSnapshots(profileId)).thenReturn(List.of());
        when(aliasRepository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of());
        when(matcher.match(List.of(), Set.of()))
                .thenReturn(new ImportInternalTransferMatcher.MatchResult(List.of(), List.of(pair)));
        when(matchRepository.findByDebitImportRowIdAndCreditImportRowId(debitRowId, creditRowId))
                .thenReturn(Optional.empty());
        when(referenceRepository.findByProfileIdAndImportRowId(profileId, debitRowId))
                .thenReturn(Optional.of(TransactionImportReference.builder().transactionId(debitTransactionId).build()));
        when(referenceRepository.findByProfileIdAndImportRowId(profileId, creditRowId))
                .thenReturn(Optional.of(TransactionImportReference.builder().transactionId(creditTransactionId).build()));
        when(matchRepository.save(any())).thenAnswer(invocation -> {
            var match = invocation.getArgument(0, ImportInternalTransferMatch.class);
            match.setId(groupId);
            return match;
        });
        var debitTransaction = MoneyTransaction.builder().profileId(profileId).build();
        var creditTransaction = MoneyTransaction.builder().profileId(profileId).build();
        when(transactionRepository.findByIdAndProfileId(debitTransactionId, profileId))
                .thenReturn(Optional.of(debitTransaction));
        when(transactionRepository.findByIdAndProfileId(creditTransactionId, profileId))
                .thenReturn(Optional.of(creditTransaction));

        service.applyToBatch(profileId, batchId);

        assertEquals(groupId, debitTransaction.getInternalTransferGroupId());
        assertEquals(groupId, creditTransaction.getInternalTransferGroupId());
        assertEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, debitTransaction.getBalanceImpact());
        assertEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, creditTransaction.getBalanceImpact());
        verify(transactionRepository, never()).findById(any());
    }
}
