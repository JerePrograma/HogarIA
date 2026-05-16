package com.hogaria.service;

import com.hogaria.dto.PlanningDtos.HabitCheckinRequest;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.Habit;
import com.hogaria.entity.HabitCheckin;
import com.hogaria.entity.HabitFrequency;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.HabitCheckinRepository;
import com.hogaria.repository.HabitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {
    @Mock HabitRepository repo;
    @Mock HabitCheckinRepository checkRepo;
    @Mock FinancialProfileRepository profileRepository;
    @InjectMocks HabitService service;

    @Test
    void checkRejectsFutureDate() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID habitId = UUID.randomUUID();

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        assertThrows(IllegalArgumentException.class, () -> service.check(userId, profileId, habitId, LocalDate.now().plusDays(1), new HabitCheckinRequest(true, null)));
    }

    @Test
    void checkRejectsWeeklyDuplicateInSameWeek() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID habitId = UUID.randomUUID();
        LocalDate date = LocalDate.now().minusDays(1);

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(repo.findById(habitId)).thenReturn(Optional.of(Habit.builder().id(habitId).profileId(profileId).frequency(HabitFrequency.WEEKLY).build()));
        when(checkRepo.findAll()).thenReturn(List.of(HabitCheckin.builder().habitId(habitId).checkinDate(date.minusDays(1)).build()));

        assertThrows(IllegalArgumentException.class, () -> service.check(userId, profileId, habitId, date, new HabitCheckinRequest(true, null)));
    }
}
