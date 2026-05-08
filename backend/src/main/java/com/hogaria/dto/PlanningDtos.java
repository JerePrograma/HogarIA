package com.hogaria.dto;
import com.hogaria.entity.*;import jakarta.validation.constraints.*;import java.math.*;import java.time.*;import java.util.*;
public class PlanningDtos {
public record FinancialGoalCreateRequest(@NotBlank String name,@NotNull GoalType goalType,@NotNull @DecimalMin(value="0.01") BigDecimal targetAmount,@NotNull @DecimalMin(value="0.00") BigDecimal currentAmount, BigDecimal monthlyTargetAmount, LocalDate targetDate,@NotNull @Min(1) @Max(5) Integer priority,String notes){}
public record FinancialGoalResponse(UUID id,UUID profileId,String name,GoalType goalType,BigDecimal targetAmount,BigDecimal currentAmount,BigDecimal progress,GoalStatus status){}
public record EmergencyFundRequest(@Min(3) @Max(6) int coverageMonths){}
public record HabitCreateRequest(@NotBlank String name,String description,@NotNull HabitFrequency frequency){}
public record HabitResponse(UUID id,UUID profileId,String name,HabitFrequency frequency,Boolean active){}
public record HabitCheckinRequest(Boolean done,String notes){}
public record HabitCheckinResponse(UUID id,UUID habitId,LocalDate checkDate,Boolean done){}
public record InflationIndexCreateRequest(@Min(2000) @Max(2100) Integer year,@Min(1) @Max(12) Integer month,@NotNull BigDecimal rate,@NotNull InflationSource source,Boolean observed,String notes){}
public record InflationIndexResponse(UUID id,Integer year,Integer month,BigDecimal rate,InflationSource source,Boolean observed){}
public record InflationAccumulatedResponse(BigDecimal accumulatedRate){}
}
