package com.hogaria.domains.transactions.lifecycle;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.dto.TransactionDeletionResponse;
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
    public TransactionDeletionResponse delete(MoneyTransaction transaction, UUID userId) {
        var decision = deletionPolicy.decide(transaction);
        var unlinkResult = handleLinks(
                transaction,
                decision
        );

        deletionObserver.beforeTransactionRemoval(transaction, decision, unlinkResult);

        try {
            if (decision.mode() == TransactionDeletionDecision.DeletionMode.SOFT_IGNORE) {
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
                "transaction_delete operation=delete userId={} profileId={} transactionId={} mode={} linkHandling={} reason={} linkedItemsUpdated={} matchesDeleted={} systemConversionMatchesDeleted={}",
                userId,
                transaction.getProfileId(),
                transaction.getId(),
                decision.mode(),
                decision.linkHandling(),
                decision.code(),
                unlinkResult.linkedItemsUpdated(),
                unlinkResult.matchesDeleted(),
                unlinkResult.systemConversionMatchesDeleted()
        );

        return toResponse(transaction, decision, unlinkResult);
    }

    private MonthlyPlanTransactionUnlinkResult handleLinks(
            MoneyTransaction transaction,
            TransactionDeletionDecision decision
    ) {
        if (decision.linkHandling() == TransactionDeletionDecision.LinkHandling.KEEP_LINKS) {
            return new MonthlyPlanTransactionUnlinkResult(0, 0, 0);
        }

        if (decision.linkHandling() == TransactionDeletionDecision.LinkHandling.BLOCK_IF_LINKED) {
            var linkSummary = monthlyPlanTransactionLinkService.describeLinks(
                    transaction.getProfileId(),
                    transaction.getId()
            );

            if (linkSummary.hasLinks()) {
                throw new DomainConflictException(
                        "No se puede eliminar el movimiento porque está vinculado al plan mensual.",
                        decision.code(),
                        List.of(
                                new ErrorResponse.Detail("transactionId", transaction.getId().toString()),
                                new ErrorResponse.Detail("linkedItemsUpdated", String.valueOf(linkSummary.linkedItemsUpdated())),
                                new ErrorResponse.Detail("matchesDeleted", String.valueOf(linkSummary.matchesDeleted()))
                        )
                );
            }

            return linkSummary;
        }

        return monthlyPlanTransactionLinkService.unlinkTransaction(
                transaction.getProfileId(),
                transaction.getId()
        );
    }

    private void softIgnore(MoneyTransaction transaction, TransactionDeletionDecision decision) {
        transaction.setStatus(MoneyTransaction.Status.IGNORED);
        transaction.setClassificationStatus(MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE);
        transaction.setClassificationReason(decision.code());
        transactionRepository.saveAndFlush(transaction);
    }

    private TransactionDeletionResponse toResponse(
            MoneyTransaction transaction,
            TransactionDeletionDecision decision,
            MonthlyPlanTransactionUnlinkResult unlinkResult
    ) {
        var physicalDelete = decision.mode() == TransactionDeletionDecision.DeletionMode.PHYSICAL_DELETE;

        return new TransactionDeletionResponse(
                transaction.getId(),
                TransactionDeletionResponse.Mode.valueOf(decision.mode().name()),
                decision.code(),
                physicalDelete
                        ? "Movimiento eliminado correctamente."
                        : "Movimiento ignorado para preservar trazabilidad.",
                unlinkResult.linkedItemsUpdated(),
                unlinkResult.matchesDeleted(),
                unlinkResult.systemConversionMatchesDeleted(),
                physicalDelete ? null : transaction.getStatus(),
                physicalDelete ? null : transaction.getClassificationStatus()
        );
    }
}
