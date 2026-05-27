package com.hogaria.service;

import com.hogaria.dto.TransactionReviewDtos.InternalTransferCandidate;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferPreviewRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferPreviewResponse;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalTransferMatcherService {

    private final FinancialProfileRepository profileRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final TransactionReviewMapper mapper;

    public InternalTransferMatcherService(
            FinancialProfileRepository profileRepository,
            MoneyTransactionRepository transactionRepository,
            TransactionReviewMapper mapper
    ) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public InternalTransferPreviewResponse preview(UUID userId, UUID profileId, InternalTransferPreviewRequest request) {
        ensureProfile(userId, profileId);

        var transactions = loadTransactions(profileId, request.year(), request.month()).stream()
                .filter(this::isCandidateLeg)
                .sorted(Comparator.comparing(MoneyTransaction::getRealDate))
                .toList();

        int toleranceDays = request.toleranceDays() == null ? 2 : Math.max(0, request.toleranceDays());
        var candidates = new ArrayList<InternalTransferCandidate>();

        for (int i = 0; i < transactions.size(); i++) {
            var left = transactions.get(i);
            for (int j = i + 1; j < transactions.size(); j++) {
                var right = transactions.get(j);
                if (sameAccountOrCurrencyMismatch(left, right)) {
                    continue;
                }

                var dayDistance = Math.abs(ChronoUnit.DAYS.between(left.getRealDate(), right.getRealDate()));
                if (dayDistance > toleranceDays) {
                    continue;
                }

                var amountDifference = left.getAmount().subtract(right.getAmount()).abs();
                if (amountDifference.signum() != 0) {
                    continue;
                }

                if (!looksLikeInternalTransfer(left, right)) {
                    continue;
                }

                var debitLeg = chooseDebitLeg(left, right);
                var creditLeg = debitLeg == left ? right : left;
                candidates.add(new InternalTransferCandidate(
                        mapper.toItem(debitLeg),
                        mapper.toItem(creditLeg),
                        amountDifference,
                        dayDistance,
                        "Misma moneda, monto exacto, cuentas distintas y fecha cercana."
                ));
            }
        }

        return new InternalTransferPreviewResponse(candidates.size(), candidates);
    }

    private List<MoneyTransaction> loadTransactions(UUID profileId, Integer year, Integer month) {
        if (year != null && month != null) {
            var from = LocalDate.of(year, month, 1);
            var to = from.withDayOfMonth(from.lengthOfMonth());
            return transactionRepository.findByProfileIdAndRealDateBetween(profileId, from.minusDays(2), to.plusDays(2));
        }

        return transactionRepository.findByProfileId(profileId);
    }

    private boolean isCandidateLeg(MoneyTransaction transaction) {
        return transaction.getStatus() != MoneyTransaction.Status.IGNORED
                && transaction.getInternalTransferGroupId() == null
                && transaction.getAmount() != null
                && transaction.getAmount().signum() > 0;
    }

    private boolean sameAccountOrCurrencyMismatch(MoneyTransaction left, MoneyTransaction right) {
        return left.getAccountId().equals(right.getAccountId())
                || !left.getCurrency().equalsIgnoreCase(right.getCurrency());
    }

    private boolean looksLikeInternalTransfer(MoneyTransaction left, MoneyTransaction right) {
        return transferSignals(left) || transferSignals(right)
                || left.getMovementType() == MoneyTransaction.MovementType.TRANSFER
                || right.getMovementType() == MoneyTransaction.MovementType.TRANSFER;
    }

    private boolean transferSignals(MoneyTransaction transaction) {
        var text = ((transaction.getNormalizedDescription() == null ? "" : transaction.getNormalizedDescription())
                + " "
                + (transaction.getDescription() == null ? "" : transaction.getDescription())
                + " "
                + (transaction.getClassificationReason() == null ? "" : transaction.getClassificationReason()))
                .toUpperCase(Locale.ROOT);

        return text.contains("DEBIN")
                || text.contains("CUENTA DNI")
                || text.contains("TRANSFER")
                || text.contains("TRASPASO")
                || text.contains("FONDEO")
                || text.contains("MERCADO PAGO")
                || text.contains("INTERNAL_TRANSFER");
    }

    private MoneyTransaction chooseDebitLeg(MoneyTransaction left, MoneyTransaction right) {
        if (left.getMovementType() == MoneyTransaction.MovementType.EXPENSE
                && right.getMovementType() == MoneyTransaction.MovementType.INCOME) {
            return left;
        }
        if (right.getMovementType() == MoneyTransaction.MovementType.EXPENSE
                && left.getMovementType() == MoneyTransaction.MovementType.INCOME) {
            return right;
        }
        return left.getRealDate().isBefore(right.getRealDate()) ? left : right;
    }

    private void ensureProfile(UUID userId, UUID profileId) {
        profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
    }
}
