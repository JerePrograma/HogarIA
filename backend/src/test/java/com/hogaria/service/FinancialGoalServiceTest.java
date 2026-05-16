package com.hogaria.service;

import com.hogaria.dto.PlanningDtos.FinancialGoalCreateRequest;
import com.hogaria.entity.FinancialGoal;
import com.hogaria.entity.GoalType;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialGoalRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialGoalServiceTest {

    @Mock FinancialGoalRepository repo;
    @Mock FinancialProfileRepository profileRepository;
    @Mock MoneyTransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;
    @InjectMocks FinancialGoalService service;

    @Test
    void createRejectsForeignProfile() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.create(userId, profileId,
                new FinancialGoalCreateRequest("Meta", GoalType.OTHER, new BigDecimal("100"), BigDecimal.ZERO, null, null, null, null)));
    }

    @Test
    void createComputesCompletedStatusWhenCurrentReachesTarget() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(repo.save(any(FinancialGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(userId, profileId,
                new FinancialGoalCreateRequest("Meta", GoalType.SAVING_TARGET, new BigDecimal("100"), new BigDecimal("100"), null, null, null, null));

        assertEquals(profileId, response.profileId());
        assertEquals("COMPLETED", response.status().name());
    }
}
