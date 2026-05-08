package com.hogaria.dto;

import com.hogaria.entity.GoalStatus;
import com.hogaria.entity.GoalType;
import com.hogaria.entity.HabitFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlanningDtos {

    public record FinancialGoalCreateRequest(
            @NotBlank String name,
            @NotNull GoalType goalType,
            @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
            @DecimalMin(value = "0.00") BigDecimal currentAmount,
            @DecimalMin(value = "0.00") BigDecimal monthlyContribution,
            LocalDate targetDate,
            GoalStatus status,
            String notes
    ) {}

    public record FinancialGoalResponse(
            UUID id,
            UUID profileId,
            String name,
            GoalType goalType,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            BigDecimal monthlyContribution,
            LocalDate targetDate,
            GoalStatus status,
            String notes,
            BigDecimal progressPercent,
            Integer monthsRemaining,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record EmergencyFundRequest(
            @Min(3) @Max(6) int coverageMonths
    ) {}

    public record HabitCreateRequest(
            @NotBlank String description,
            String area,
            @NotNull HabitFrequency frequency
    ) {}

    public record HabitResponse(
            UUID id,
            UUID profileId,
            String description,
            String area,
            HabitFrequency frequency,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record HabitCheckinRequest(
            Boolean completed,
            String note
    ) {}

    public record HabitCheckinResponse(
            UUID id,
            UUID habitId,
            LocalDate checkinDate,
            Boolean completed,
            String note,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record InflationIndexCreateRequest(
            @Min(2000) @Max(2100) Integer year,
            @Min(1) @Max(12) Integer month,
            String categoryCode,
            String categoryName,
            @NotNull BigDecimal monthlyRate,
            String source,
            Boolean projection
    ) {}

    public record InflationIndexResponse(
            UUID id,
            Integer year,
            Integer month,
            String categoryCode,
            String categoryName,
            BigDecimal monthlyRate,
            String source,
            Boolean projection,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record InflationAccumulatedResponse(
            BigDecimal accumulatedRate
    ) {}
}