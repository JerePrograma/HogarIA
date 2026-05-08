package com.hogaria.service;

import com.hogaria.dto.PlanningDtos.EmergencyFundRequest;
import com.hogaria.dto.PlanningDtos.FinancialGoalCreateRequest;
import com.hogaria.dto.PlanningDtos.FinancialGoalResponse;
import com.hogaria.entity.FinancialGoal;
import com.hogaria.entity.GoalStatus;
import com.hogaria.entity.GoalType;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialGoalRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository repo;
    private final FinancialProfileRepository profileRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public FinancialGoalService(
            FinancialGoalRepository repo,
            FinancialProfileRepository profileRepository,
            MoneyTransactionRepository transactionRepository,
            CategoryRepository categoryRepository
    ) {
        this.repo = repo;
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    private void own(UUID userId, UUID profileId) {
        profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> list(UUID userId, UUID profileId) {
        own(userId, profileId);

        return repo.findByProfileId(profileId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FinancialGoalResponse create(UUID userId, UUID profileId, FinancialGoalCreateRequest request) {
        own(userId, profileId);

        BigDecimal currentAmount = request.currentAmount() != null
                ? request.currentAmount()
                : BigDecimal.ZERO;

        GoalStatus status = request.status() != null
                ? request.status()
                : currentAmount.compareTo(request.targetAmount()) >= 0
                  ? GoalStatus.COMPLETED
                  : GoalStatus.ACTIVE;

        FinancialGoal goal = FinancialGoal.builder()
                .profileId(profileId)
                .name(request.name())
                .goalType(request.goalType())
                .targetAmount(request.targetAmount())
                .currentAmount(currentAmount)
                .monthlyContribution(request.monthlyContribution())
                .targetDate(request.targetDate())
                .status(status)
                .notes(request.notes())
                .build();

        return toResponse(repo.save(goal));
    }

    @Transactional
    public void delete(UUID userId, UUID profileId, UUID id) {
        own(userId, profileId);

        FinancialGoal goal = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Objetivo no encontrado."));

        if (!goal.getProfileId().equals(profileId)) {
            throw new ForbiddenException("El objetivo no pertenece al perfil indicado.");
        }

        repo.delete(goal);
    }

    @Transactional
    public FinancialGoalResponse emergencyFund(UUID userId, UUID profileId, int months) {
        own(userId, profileId);

        if (months < 3 || months > 6) {
            throw new IllegalArgumentException("La cobertura debe estar entre 3 y 6 meses.");
        }

        List<MoneyTransaction> transactions = transactionRepository.findByProfileId(profileId);

        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getMovementType() == MoneyTransaction.MovementType.EXPENSE)
                .map(MoneyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal target = totalExpenses.multiply(BigDecimal.valueOf(months));

        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            target = new BigDecimal("0.01");
        }

        FinancialGoal goal = FinancialGoal.builder()
                .profileId(profileId)
                .name("Fondo de emergencia")
                .goalType(GoalType.EMERGENCY_FUND)
                .targetAmount(target)
                .currentAmount(BigDecimal.ZERO)
                .monthlyContribution(null)
                .status(GoalStatus.ACTIVE)
                .notes("Objetivo sugerido automáticamente para cubrir " + months + " meses de gastos.")
                .build();

        return toResponse(repo.save(goal));
    }

    private FinancialGoalResponse toResponse(FinancialGoal goal) {
        BigDecimal progressPercent = goal.getTargetAmount() == null
                || goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : goal.getCurrentAmount()
                  .multiply(new BigDecimal("100"))
                  .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);

        Integer monthsRemaining = null;

        if (
                goal.getMonthlyContribution() != null
                        && goal.getMonthlyContribution().compareTo(BigDecimal.ZERO) > 0
                        && goal.getTargetAmount() != null
                        && goal.getCurrentAmount() != null
                        && goal.getTargetAmount().compareTo(goal.getCurrentAmount()) > 0
        ) {
            BigDecimal pending = goal.getTargetAmount().subtract(goal.getCurrentAmount());

            monthsRemaining = pending
                    .divide(goal.getMonthlyContribution(), 0, RoundingMode.CEILING)
                    .intValue();
        }

        return new FinancialGoalResponse(
                goal.getId(),
                goal.getProfileId(),
                goal.getName(),
                goal.getGoalType(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getMonthlyContribution(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getNotes(),
                progressPercent,
                monthsRemaining,
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}