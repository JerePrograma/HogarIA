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
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
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

        Habit habit = repo.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getProfileId().equals(profileId)) {
            throw new ForbiddenException("Habit not in profile");
        }

        HabitCheckin checkin = checkRepo.findByHabitIdAndCheckinDate(habitId, date)
                .orElseGet(() -> HabitCheckin.builder()
                        .habitId(habitId)
                        .checkinDate(date)
                        .build());

        checkin.setCompleted(Boolean.TRUE.equals(request.completed()));
        checkin.setNote(request.note());

        return toResponse(checkRepo.save(checkin));
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