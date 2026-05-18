package com.hogaria.controller;
import com.hogaria.dto.DashboardDtos.DashboardSummaryResponse;import com.hogaria.security.CurrentUserResolver;import com.hogaria.service.DashboardService;import java.util.UUID;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class DashboardController {
  private final DashboardService service; private final CurrentUserResolver parser;
  public DashboardController(DashboardService service, CurrentUserResolver parser){this.service=service;this.parser=parser;}
  @GetMapping("/profiles/{profileId}/dashboard/monthly") public DashboardSummaryResponse monthly(@RequestHeader(value = "X-User-Id", required = false) String h,@PathVariable UUID profileId,@RequestParam int year,@RequestParam int month){return service.getMonthlySummary(parser.parse(h),profileId,year,month);} }
