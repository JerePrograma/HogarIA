package com.hogaria.service;

import com.hogaria.dto.TransactionReviewDtos.InternalTransferLinkRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferLinkResponse;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferUnlinkRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferUnlinkResponse;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalTransferResolutionService {

    private final FinancialProfileRepository profileRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionReviewMapper mapper;
    private final TransactionFinancialImpactService impactService;

    public InternalTransferResolutionService(
            FinancialProfileRepository profileRepository,
            MoneyTransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            TransactionReviewMapper mapper,
            TransactionFinancialImpactService impactService
    ) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.impactService = impactService;
    }

    @Transactional
    public InternalTransferLinkResponse link(UUID userId, UUID profileId, InternalTransferLinkRequest request) {
        ensureProfile(userId, profileId);

        var debit = load(profileId, request.debitTransactionId());
        var credit = load(profileId, request.creditTransactionId());
        validateCanLink(debit, credit, request);

        var groupId = UUID.randomUUID();
        markInternalTransfer(debit, groupId);
        markInternalTransfer(credit, groupId);

        var savedDebit = transactionRepository.save(debit);
        var savedCredit = transactionRepository.save(credit);

        return new InternalTransferLinkResponse(groupId, mapper.toItem(savedDebit), mapper.toItem(savedCredit));
    }

    @Transactional
    public InternalTransferUnlinkResponse unlink(UUID userId, UUID profileId, InternalTransferUnlinkRequest request) {
        ensureProfile(userId, profileId);

        List<MoneyTransaction> transactions;
        if (request.internalTransferGroupId() != null) {
            transactions = transactionRepository.findByProfileIdAndInternalTransferGroupId(profileId, request.internalTransferGroupId());
        } else if (request.transactionIds() != null && !request.transactionIds().isEmpty()) {
            transactions = transactionRepository.findByProfileIdAndIdIn(profileId, request.transactionIds());
        } else {
            throw new BadRequestException("Debe indicar internalTransferGroupId o transactionIds.");
        }

        var ids = transactions.stream().map(MoneyTransaction::getId).toList();
        for (var transaction : transactions) {
            transaction.setInternalTransferGroupId(null);
            transaction.setPaymentChannel(MoneyTransaction.PaymentChannel.UNKNOWN);
            transaction.setClassificationStatus(MoneyTransaction.ClassificationStatus.REVIEW);
            transaction.setClassificationReason("INTERNAL_TRANSFER_UNLINKED");
            transaction.setBalanceImpact(recalculateImpact(transaction));
        }
        transactionRepository.saveAll(transactions);

        return new InternalTransferUnlinkResponse(transactions.size(), ids);
    }

    private MoneyTransaction load(UUID profileId, UUID transactionId) {
        return transactionRepository.findByIdAndProfileId(transactionId, profileId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    private void validateCanLink(MoneyTransaction debit, MoneyTransaction credit, InternalTransferLinkRequest request) {
        if (Objects.equals(debit.getId(), credit.getId())) {
            throw new BadRequestException("No se puede vincular un movimiento consigo mismo.");
        }
        if (!Objects.equals(debit.getProfileId(), credit.getProfileId())) {
            throw new BadRequestException("Ambos movimientos deben pertenecer al mismo perfil.");
        }
        if (Objects.equals(debit.getAccountId(), credit.getAccountId())) {
            throw new BadRequestException("Una transferencia interna requiere cuentas distintas.");
        }
        if (!debit.getCurrency().equalsIgnoreCase(credit.getCurrency())) {
            throw new BadRequestException("Las monedas deben coincidir.");
        }
        if (debit.getInternalTransferGroupId() != null || credit.getInternalTransferGroupId() != null) {
            throw new BadRequestException("Alguna pata ya pertenece a una transferencia interna activa.");
        }

        var toleranceAmount = request.toleranceAmount() == null ? BigDecimal.ZERO : request.toleranceAmount().abs();
        var diff = debit.getAmount().subtract(credit.getAmount()).abs();
        if (diff.compareTo(toleranceAmount) > 0) {
            throw new BadRequestException("Los montos no coinciden dentro de la tolerancia.");
        }

        var toleranceDays = request.toleranceDays() == null ? 2 : Math.max(0, request.toleranceDays());
        var dayDistance = Math.abs(ChronoUnit.DAYS.between(debit.getRealDate(), credit.getRealDate()));
        if (dayDistance > toleranceDays) {
            throw new BadRequestException("Las fechas están fuera de la tolerancia permitida.");
        }
    }

    private void markInternalTransfer(MoneyTransaction transaction, UUID groupId) {
        transaction.setInternalTransferGroupId(groupId);
        transaction.setMovementType(MoneyTransaction.MovementType.TRANSFER);
        transaction.setPaymentChannel(MoneyTransaction.PaymentChannel.INTERNAL_TRANSFER);
        transaction.setClassificationStatus(MoneyTransaction.ClassificationStatus.TECHNICAL);
        transaction.setClassificationReason("USER_MARKED_INTERNAL_TRANSFER");
        transaction.setBalanceImpact(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER);
    }

    private MoneyTransaction.BalanceImpact recalculateImpact(MoneyTransaction transaction) {
        Category category = transaction.getCategoryId() == null
                ? null
                : categoryRepository.findById(transaction.getCategoryId()).orElse(null);
        return impactService.analyze(transaction, category, null).balanceImpact();
    }

    private void ensureProfile(UUID userId, UUID profileId) {
        profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
    }
}
