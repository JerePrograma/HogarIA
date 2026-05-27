package com.hogaria.service;

import com.hogaria.dto.TransactionReviewDtos.DuplicateGroup;
import com.hogaria.dto.TransactionReviewDtos.DuplicatePreviewRequest;
import com.hogaria.dto.TransactionReviewDtos.DuplicatePreviewResponse;
import com.hogaria.dto.TransactionReviewDtos.DuplicateResolveRequest;
import com.hogaria.dto.TransactionReviewDtos.DuplicateResolveResponse;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionDuplicateReviewService {

    private final FinancialProfileRepository profileRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final TransactionReviewMapper mapper;

    public TransactionDuplicateReviewService(
            FinancialProfileRepository profileRepository,
            MoneyTransactionRepository transactionRepository,
            TransactionReviewMapper mapper
    ) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DuplicatePreviewResponse preview(UUID userId, UUID profileId, DuplicatePreviewRequest request) {
        ensureProfile(userId, profileId);

        var transactions = loadTransactions(profileId, request.year(), request.month());
        var groups = new ArrayList<DuplicateGroup>();
        groups.addAll(exactGroups(transactions));
        groups.addAll(crossSourceGroups(transactions));

        var exactCount = (int) groups.stream().filter(group -> group.groupType().equals("EXACT_DUPLICATE")).count();
        var crossCount = (int) groups.stream().filter(group -> group.groupType().equals("POSSIBLE_CROSS_SOURCE_DUPLICATE")).count();

        return new DuplicatePreviewResponse(exactCount, crossCount, groups);
    }

    @Transactional
    public DuplicateResolveResponse resolve(UUID userId, UUID profileId, DuplicateResolveRequest request) {
        ensureProfile(userId, profileId);

        if (request.keepTransactionId() == null
                || request.duplicateTransactionIds() == null
                || request.duplicateTransactionIds().isEmpty()) {
            throw new BadRequestException("Debe indicar el movimiento a conservar y duplicados a ignorar.");
        }

        var keep = transactionRepository.findByIdAndProfileId(request.keepTransactionId(), profileId)
                .orElseThrow(() -> new NotFoundException("Transaction to keep not found"));

        var duplicates = transactionRepository.findByProfileIdAndIdIn(profileId, request.duplicateTransactionIds());
        if (duplicates.size() != request.duplicateTransactionIds().size()) {
            throw new BadRequestException("Todos los duplicados deben pertenecer al perfil.");
        }

        for (var duplicate : duplicates) {
            if (duplicate.getId().equals(keep.getId())) {
                throw new BadRequestException("El movimiento conservado no puede estar dentro de duplicateTransactionIds.");
            }
            duplicate.setStatus(MoneyTransaction.Status.IGNORED);
            duplicate.setClassificationStatus(MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE);
            duplicate.setClassificationReason("DUPLICATE_RESOLVED_KEEP:" + keep.getId());
            duplicate.setBalanceImpact(MoneyTransaction.BalanceImpact.IGNORED);
        }
        transactionRepository.saveAll(duplicates);

        return new DuplicateResolveResponse(
                keep.getId(),
                duplicates.size(),
                duplicates.stream().map(MoneyTransaction::getId).toList()
        );
    }

    private List<MoneyTransaction> loadTransactions(UUID profileId, Integer year, Integer month) {
        if (year != null && month != null) {
            var from = LocalDate.of(year, month, 1);
            var to = from.withDayOfMonth(from.lengthOfMonth());
            return transactionRepository.findByProfileIdAndBudgetDateBetween(profileId, from, to);
        }

        return transactionRepository.findByProfileId(profileId);
    }

    private List<DuplicateGroup> exactGroups(List<MoneyTransaction> transactions) {
        return transactions.stream()
                .filter(tx -> tx.getDuplicateFingerprint() != null && tx.getStatus() != MoneyTransaction.Status.IGNORED)
                .collect(Collectors.groupingBy(
                        tx -> tx.getAccountId() + "|" + tx.getDuplicateFingerprint(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> group("EXACT_DUPLICATE", entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DuplicateGroup> crossSourceGroups(List<MoneyTransaction> transactions) {
        return transactions.stream()
                .filter(tx -> "POSSIBLE_CROSS_SOURCE_DUPLICATE".equals(tx.getClassificationReason()))
                .collect(Collectors.groupingBy(
                        tx -> tx.getRealDate() + "|" + tx.getAmount() + "|" + tx.getCurrency(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> group("POSSIBLE_CROSS_SOURCE_DUPLICATE", entry.getKey(), entry.getValue()))
                .toList();
    }

    private DuplicateGroup group(String type, String key, List<MoneyTransaction> transactions) {
        var first = transactions.get(0);
        return new DuplicateGroup(
                type,
                key,
                first.getAmount(),
                first.getCurrency(),
                transactions.stream().map(mapper::toItem).toList()
        );
    }

    private void ensureProfile(UUID userId, UUID profileId) {
        profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
    }
}
