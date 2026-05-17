package com.hogaria.service;

import com.hogaria.dto.PlanningDtos.HabitCheckinRequest;
import com.hogaria.dto.PlanningDtos.HabitCheckinResponse;
import com.hogaria.dto.PlanningDtos.HabitCreateRequest;
import com.hogaria.dto.PlanningDtos.HabitResponse;
import com.hogaria.entity.Habit;
import com.hogaria.entity.HabitCheckin;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.HabitCheckinRepository;
import com.hogaria.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.UUID;

@Service
public class HabitService {

    private final HabitRepository repo;
    private final HabitCheckinRepository checkRepo;
    private final FinancialProfileRepository profileRepository;

    public HabitService(
            HabitRepository repo,
            HabitCheckinRepository checkRepo,
            FinancialProfileRepository profileRepository
    ) {
        this.repo = repo;
        this.checkRepo = checkRepo;
        this.profileRepository = profileRepository;
    }

    private void own(UUID userId, UUID profileId) {
        profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> list(UUID userId, UUID profileId) {
        own(userId, profileId);

        return repo.findByProfileId(profileId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HabitResponse create(UUID userId, UUID profileId, HabitCreateRequest request) {
        own(userId, profileId);

        Habit habit = Habit.builder()
                .profileId(profileId)
                .description(request.description())
                .area(request.area() != null ? request.area() : "FINANZAS")
                .frequency(request.frequency())
                .active(true)
                .build();

        return toResponse(repo.save(habit));
    }

    @Transactional
    public HabitCheckinResponse check(
            UUID userId,
            UUID profileId,
            UUID habitId,
            LocalDate date,
            HabitCheckinRequest request
    ) {
        own(userId, profileId);
        validateCheckinDate(date);

        Habit habit = repo.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Hábito no encontrado."));

        if (!habit.getProfileId().equals(profileId)) {
            throw new ForbiddenException("El hábito no pertenece al perfil indicado.");
        }

        validateFrequency(habit, date);

        HabitCheckin checkin = checkRepo.findByHabitIdAndCheckinDate(habitId, date)
                .orElseGet(() -> HabitCheckin.builder()
                        .habitId(habitId)
                        .checkinDate(date)
                        .build());

        checkin.setCompleted(Boolean.TRUE.equals(request.completed()));
        checkin.setNote(request.note());

        return toResponse(checkRepo.save(checkin));
    }

    private void validateCheckinDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha del check-in es obligatoria.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("No se permiten check-ins en fechas futuras.");
        }
    }

    private void validateFrequency(Habit habit, LocalDate date) {
        if (habit.getFrequency() == null) {
            throw new IllegalArgumentException("La frecuencia del hábito es obligatoria.");
        }
        if (habit.getFrequency().name().equals("WEEKLY")) {
            LocalDate start = date.with(WeekFields.ISO.dayOfWeek(), 1);
            LocalDate end = start.plusDays(6);
            boolean alreadyCheckedThisWeek = checkRepo.existsByHabitIdAndCheckinDateBetweenAndCheckinDateNot(
                    habit.getId(),
                    start,
                    end,
                    date
            );
            if (alreadyCheckedThisWeek) {
                throw new IllegalArgumentException("El hábito semanal ya tiene check-in en esta semana.");
            }
        }
        if (habit.getFrequency().name().equals("MONTHLY")) {
            LocalDate start = date.withDayOfMonth(1);
            LocalDate end = date.withDayOfMonth(date.lengthOfMonth());
            boolean alreadyCheckedThisMonth = checkRepo.existsByHabitIdAndCheckinDateBetweenAndCheckinDateNot(
                    habit.getId(),
                    start,
                    end,
                    date
            );
            if (alreadyCheckedThisMonth) {
                throw new IllegalArgumentException("El hábito mensual ya tiene check-in en este mes.");
            }
        }
    }

    private HabitResponse toResponse(Habit habit) {
        return new HabitResponse(
                habit.getId(),
                habit.getProfileId(),
                habit.getDescription(),
                habit.getArea(),
                habit.getFrequency(),
                habit.getActive(),
                habit.getCreatedAt(),
                habit.getUpdatedAt()
        );
    }

    private HabitCheckinResponse toResponse(HabitCheckin checkin) {
        return new HabitCheckinResponse(
                checkin.getId(),
                checkin.getHabitId(),
                checkin.getCheckinDate(),
                checkin.getCompleted(),
                checkin.getNote(),
                checkin.getCreatedAt(),
                checkin.getUpdatedAt()
        );
    }
}
