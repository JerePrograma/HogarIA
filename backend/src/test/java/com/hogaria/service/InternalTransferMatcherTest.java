package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionReviewDtos.InternalTransferLinkRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferPreviewRequest;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalTransferMatcherTest {

    @Mock FinancialProfileRepository profileRepository;
    @Mock MoneyTransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;

    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    UUID banco = UUID.randomUUID();
    UUID mp = UUID.randomUUID();

    InternalTransferMatcherService matcherService;
    InternalTransferResolutionService resolutionService;
    TransactionFinancialImpactService impactService = new TransactionFinancialImpactService();

    @BeforeEach
    void setup() {
        var mapper = new TransactionReviewMapper();
        matcherService = new InternalTransferMatcherService(profileRepository, transactionRepository, mapper);
        resolutionService = new InternalTransferResolutionService(profileRepository, transactionRepository, categoryRepository, mapper, impactService);
        org.mockito.Mockito.lenient()
                .when(profileRepository.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(new FinancialProfile()));
    }

    @Test
    void mercadoPagoSalidaAndBancoProvinciaEntradaSameAmountAreCandidate() {
        var date = LocalDate.of(2026, 5, 7);
        var salidaMp = tx(mp, MoneyTransaction.MovementType.EXPENSE, "Pago Debin Mercado Pago", date);
        var entradaBanco = tx(banco, MoneyTransaction.MovementType.INCOME, "CR.TRAN. cuenta propia", date.plusDays(1));
        when(transactionRepository.findByProfileIdAndRealDateBetween(profileId, date.withDayOfMonth(1).minusDays(2), date.withDayOfMonth(31).plusDays(2)))
                .thenReturn(List.of(salidaMp, entradaBanco));

        var preview = matcherService.preview(userId, profileId, new InternalTransferPreviewRequest(2026, 5, null));

        assertEquals(1, preview.candidateCount());
        assertEquals(salidaMp.getId(), preview.candidates().get(0).debitLeg().id());
        assertEquals(entradaBanco.getId(), preview.candidates().get(0).creditLeg().id());
    }

    @Test
    void linkedInternalTransferDoesNotImpactIncomeExpenseOrOperationalBalance() {
        var tx = tx(mp, MoneyTransaction.MovementType.TRANSFER, "Transferencia interna", LocalDate.of(2026, 5, 7));
        tx.setInternalTransferGroupId(UUID.randomUUID());
        tx.setClassificationStatus(MoneyTransaction.ClassificationStatus.TECHNICAL);

        var impact = impactService.analyze(tx, null, null);

        assertTrue(impact.internalTransfer());
        assertFalse(impact.impactsIncome());
        assertFalse(impact.impactsConsumptionExpense());
        assertFalse(impact.impactsOperationalBalance());
    }

    @Test
    void oneLegOnlyStaysUnmatchedReviewCandidate() {
        var date = LocalDate.of(2026, 5, 7);
        when(transactionRepository.findByProfileIdAndRealDateBetween(profileId, date.withDayOfMonth(1).minusDays(2), date.withDayOfMonth(31).plusDays(2)))
                .thenReturn(List.of(tx(mp, MoneyTransaction.MovementType.EXPENSE, "Pago Debin Mercado Pago", date)));

        var preview = matcherService.preview(userId, profileId, new InternalTransferPreviewRequest(2026, 5, null));

        assertEquals(0, preview.candidateCount());
    }

    @Test
    void linkedLegCannotBeLinkedToAnotherActiveGroupWithoutUnlink() {
        var left = tx(mp, MoneyTransaction.MovementType.EXPENSE, "Pago Debin", LocalDate.of(2026, 5, 7));
        var right = tx(banco, MoneyTransaction.MovementType.INCOME, "Transferencia recibida", LocalDate.of(2026, 5, 7));
        left.setInternalTransferGroupId(UUID.randomUUID());
        when(transactionRepository.findByIdAndProfileId(left.getId(), profileId)).thenReturn(Optional.of(left));
        when(transactionRepository.findByIdAndProfileId(right.getId(), profileId)).thenReturn(Optional.of(right));

        assertThrows(
                BadRequestException.class,
                () -> resolutionService.link(userId, profileId, new InternalTransferLinkRequest(left.getId(), right.getId(), BigDecimal.ZERO, 2))
        );
    }

    @Test
    void manualLinkMarksBothLegsAsTechnicalTransferWithSameGroup() {
        var left = tx(mp, MoneyTransaction.MovementType.EXPENSE, "Pago Debin", LocalDate.of(2026, 5, 7));
        var right = tx(banco, MoneyTransaction.MovementType.INCOME, "Transferencia recibida", LocalDate.of(2026, 5, 7));
        when(transactionRepository.findByIdAndProfileId(left.getId(), profileId)).thenReturn(Optional.of(left));
        when(transactionRepository.findByIdAndProfileId(right.getId(), profileId)).thenReturn(Optional.of(right));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resolutionService.link(userId, profileId, new InternalTransferLinkRequest(left.getId(), right.getId(), BigDecimal.ZERO, 2));

        assertEquals(response.internalTransferGroupId(), left.getInternalTransferGroupId());
        assertEquals(response.internalTransferGroupId(), right.getInternalTransferGroupId());
        assertEquals(MoneyTransaction.MovementType.TRANSFER, left.getMovementType());
        assertEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, right.getBalanceImpact());
    }

    private MoneyTransaction tx(UUID accountId, MoneyTransaction.MovementType type, String description, LocalDate realDate) {
        return MoneyTransaction.builder()
                .id(UUID.randomUUID())
                .profileId(profileId)
                .accountId(accountId)
                .movementType(type)
                .realDate(realDate)
                .budgetDate(realDate.withDayOfMonth(1))
                .amount(new BigDecimal("400000.00"))
                .currency("ARS")
                .description(description)
                .normalizedDescription(description.toUpperCase())
                .status(MoneyTransaction.Status.CONFIRMED)
                .classificationStatus(MoneyTransaction.ClassificationStatus.CLASSIFIED)
                .build();
    }
}
