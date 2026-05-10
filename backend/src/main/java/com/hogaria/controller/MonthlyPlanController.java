package com.hogaria.controller;
import com.hogaria.dto.MonthlyPlanDtos.*;import com.hogaria.security.CurrentUserResolver;import com.hogaria.service.MonthlyPlanService;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;import java.util.UUID;
@RestController @RequestMapping("/api")
public class MonthlyPlanController {
  private final MonthlyPlanService s; private final CurrentUserResolver cu;
  public MonthlyPlanController(MonthlyPlanService s, CurrentUserResolver cu){this.s=s;this.cu=cu;}
  @GetMapping("/profiles/{profileId}/planning/monthly") public MonthlyPlanSummaryResponse monthly(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@RequestParam int year,@RequestParam int month){ return s.summary(cu.parse(h),profileId,year,month); }
  @PostMapping("/profiles/{profileId}/planning/items") public MonthlyPlanItemResponse create(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@Valid @RequestBody MonthlyPlanItemCreateRequest r){ return s.create(cu.parse(h),profileId,r); }
  @PutMapping("/profiles/{profileId}/planning/items/{itemId}") public MonthlyPlanItemResponse update(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@PathVariable UUID itemId,@Valid @RequestBody MonthlyPlanItemUpdateRequest r){ return s.update(cu.parse(h),profileId,itemId,r); }
  @DeleteMapping("/profiles/{profileId}/planning/items/{itemId}") public void delete(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@PathVariable UUID itemId){ s.delete(cu.parse(h),profileId,itemId); }
  @PostMapping("/profiles/{profileId}/planning/items/{itemId}/convert-to-transaction") public MonthlyPlanItemResponse convert(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@PathVariable UUID itemId){ return s.convert(cu.parse(h),profileId,itemId); }
}
