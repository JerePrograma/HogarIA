package com.hogaria.repository;

import com.hogaria.entity.HabitCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface HabitCheckinRepository extends JpaRepository<HabitCheckin, UUID> {

    Optional<HabitCheckin> findByHabitIdAndCheckinDate(UUID habitId, LocalDate checkinDate);
}