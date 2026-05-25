package com.hogaria.controller;

import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionCommitRequest;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionCommitResponse;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionPreviewRequest;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionPreviewResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.BudgetPlanningSuggestionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BudgetPlanningSuggestionController {

    private final BudgetPlanningSuggestionService service;
    private final CurrentUserResolver currentUserResolver;

    public BudgetPlanningSuggestionController(
            BudgetPlanningSuggestionService service,
            CurrentUserResolver currentUserResolver
    ) {
        this.service = service;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/profiles/{profileId}/budget-planning-suggestions/preview")
    public BudgetPlanningSuggestionPreviewResponse preview(
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @PathVariable UUID profileId,
            @Valid @RequestBody BudgetPlanningSuggestionPreviewRequest request
    ) {
        return service.preview(
                currentUserResolver.parse(userHeader),
                profileId,
                request
        );
    }

    @PostMapping("/profiles/{profileId}/budget-planning-suggestions/commit")
    public BudgetPlanningSuggestionCommitResponse commit(
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @PathVariable UUID profileId,
            @Valid @RequestBody BudgetPlanningSuggestionCommitRequest request
    ) {
        return service.commit(
                currentUserResolver.parse(userHeader),
                profileId,
                request
        );
    }
}
