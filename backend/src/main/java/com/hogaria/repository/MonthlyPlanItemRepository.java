package com.hogaria.repository;
import com.hogaria.entity.MonthlyPlanItem;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface MonthlyPlanItemRepository extends JpaRepository<MonthlyPlanItem, UUID> { List<MonthlyPlanItem> findByProfileIdAndPeriodYearAndPeriodMonth(UUID profileId,Integer year,Integer month); Optional<MonthlyPlanItem> findByIdAndProfileId(UUID id,UUID profileId); }
