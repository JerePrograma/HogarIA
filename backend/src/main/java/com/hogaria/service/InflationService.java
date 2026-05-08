package com.hogaria.service;

import com.hogaria.dto.PlanningDtos.InflationAccumulatedResponse;
import com.hogaria.dto.PlanningDtos.InflationIndexCreateRequest;
import com.hogaria.dto.PlanningDtos.InflationIndexResponse;
import com.hogaria.entity.InflationIndex;
import com.hogaria.repository.InflationIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class InflationService {

    private final InflationIndexRepository repo;

    public InflationService(InflationIndexRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<InflationIndexResponse> list(Integer year) {
        return repo.findByYearOrderByMonthAsc(year)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InflationIndexResponse create(InflationIndexCreateRequest request) {
        InflationIndex index = InflationIndex.builder()
                .year(request.year())
                .month(request.month())
                .categoryCode(request.categoryCode())
                .categoryName(request.categoryName())
                .monthlyRate(request.monthlyRate())
                .source(request.source())
                .projection(Boolean.TRUE.equals(request.projection()))
                .build();

        return toResponse(repo.save(index));
    }

    @Transactional(readOnly = true)
    public InflationAccumulatedResponse acc(int fromYear, int fromMonth, int toYear, int toMonth) {
        int fromKey = fromYear * 100 + fromMonth;
        int toKey = toYear * 100 + toMonth;

        List<BigDecimal> rates = repo.findAll()
                .stream()
                .filter(i -> i.getCategoryCode() == null || i.getCategoryCode().isBlank())
                .filter(i -> {
                    int key = i.getYear() * 100 + i.getMonth();
                    return key >= fromKey && key <= toKey;
                })
                .sorted(Comparator
                        .comparing(InflationIndex::getYear)
                        .thenComparing(InflationIndex::getMonth))
                .map(InflationIndex::getMonthlyRate)
                .toList();

        BigDecimal product = BigDecimal.ONE;

        for (BigDecimal rate : rates) {
            product = product.multiply(BigDecimal.ONE.add(rate));
        }

        return new InflationAccumulatedResponse(product.subtract(BigDecimal.ONE));
    }

    private InflationIndexResponse toResponse(InflationIndex index) {
        return new InflationIndexResponse(
                index.getId(),
                index.getYear(),
                index.getMonth(),
                index.getCategoryCode(),
                index.getCategoryName(),
                index.getMonthlyRate(),
                index.getSource(),
                index.getProjection(),
                index.getCreatedAt(),
                index.getUpdatedAt()
        );
    }
}