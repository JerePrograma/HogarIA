package com.hogaria.domains.transactions.lifecycle;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.exception.ErrorResponse;
import com.hogaria.repository.MoneyTransactionRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(TransactionLifecycleService.class);

    private final MoneyTransactionRepository transactionRepository;
    private final TransactionDeletionPolicy deletionPolicy;
    private final MonthlyPlanTransactionLinkService monthlyPlanTransactionLinkService;
    private final TransactionDeletionObserver deletionObserver;

    public TransactionLifecycleService(
            MoneyTransactionRepository transactionRepository,
            TransactionDeletionPolicy deletionPolicy,
            MonthlyPlanTransactionLinkService monthlyPlanTransactionLinkService,
            TransactionDeletionObserver deletionObserver
    ) {
        this.transactionRepository = transactionRepository;
        this.deletionPolicy = deletionPolicy;
        this.monthlyPlanTransactionLinkService = monthlyPlanTransactionLinkService;
        this.deletionObserver = deletionObserver;
    }

    @Transactional
    public void delete(MoneyTransaction transaction, UUID userId) {
        var decision = deletionPolicy.decide(transaction);
        var unlinkResult = monthlyPlanTransactionLinkService.unlinkTransaction(
                transaction.getProfileId(),
                transaction.getId()
        );

        deletionObserver.beforeTransactionRemoval(transaction, decision, unlinkResult);

        try {
            if (decision.mode() == TransactionDeletionDecision.Mode.SOFT_IGNORE) {
                softIgnore(transaction, decision);
            } else {
                transactionRepository.delete(transaction);
                transactionRepository.flush();
            }
        } catch (DataIntegrityViolationException ex) {
            throw new DomainConflictException(
                    "No se puede eliminar el movimiento porque todavía existen vínculos activos.",
                    "TRANSACTION_DELETE_INTEGRITY_CONFLICT",
                    List.of(new ErrorResponse.Detail("transactionId", transaction.getId().toString())),
                    ex
            );
        }

        log.info(
                "transaction_delete operation=delete userId={} profileId={} transactionId={} mode={} reason={} linkedItemsUpdated={} matchesDeleted={} systemConversionMatchesDeleted={}",
                userId,
                transaction.getProfileId(),
                transaction.getId(),
                decision.mode(),
                decision.code(),
                unlinkResult.linkedItemsUpdated(),
                unlinkResult.matchesDeleted(),
                unlinkResult.systemConversionMatchesDeleted()
        );
    }

    private void softIgnore(MoneyTransaction transaction, TransactionDeletionDecision decision) {
        transaction.setStatus(MoneyTransaction.Status.IGNORED);
        transaction.setClassificationStatus(MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE);
        transaction.setClassificationReason(decision.code());
        transactionRepository.saveAndFlush(transaction);
    }
}
