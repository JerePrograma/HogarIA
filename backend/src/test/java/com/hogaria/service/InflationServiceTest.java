package com.hogaria.service;

import com.hogaria.dto.PlanningDtos.InflationIndexCreateRequest;
import com.hogaria.repository.InflationIndexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class InflationServiceTest {

    @Mock InflationIndexRepository repo;
    @InjectMocks InflationService service;

    @Test
    void createRejectsInvalidMonthAndProjectionNull() {
        assertThrows(IllegalArgumentException.class, () -> service.create(new InflationIndexCreateRequest(2026, 13, null, null, new BigDecimal("0.03"), "MANUAL", null)));
    }

    @Test
    void accumulatedRejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> service.acc(2026, 12, 2026, 1));
    }
}
