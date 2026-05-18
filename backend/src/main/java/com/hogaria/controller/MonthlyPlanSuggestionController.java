package com.hogaria.controller;

import com.hogaria.dto.PlanningSuggestionDtos.PlanningSuggestionRequest;
import com.hogaria.dto.PlanningSuggestionDtos.PlanningSuggestionResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.MonthlyPlanSuggestionService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MonthlyPlanSuggestionController {
  private final MonthlyPlanSuggestionService service; private final CurrentUserResolver cu;
  public MonthlyPlanSuggestionController(MonthlyPlanSuggestionService service, CurrentUserResolver cu){this.service=service;this.cu=cu;}
  @PostMapping("/profiles/{profileId}/planning/suggestions")
  public PlanningSuggestionResponse suggest(@RequestHeader(value = "X-User-Id", required = false) String h, @PathVariable UUID profileId, @RequestBody PlanningSuggestionRequest request){
    return service.suggest(cu.parse(h), profileId, request);
  }
}
