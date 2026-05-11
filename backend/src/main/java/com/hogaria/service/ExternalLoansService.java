package com.hogaria.service;

import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.integration.cjprestamos.mapper.CjPrestamosBridgeMapper;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExternalLoansService {
  private final CjPrestamosClient client;
  private final CjPrestamosProperties properties;
  private final FinancialProfileRepository profileRepository;
  private final CjPrestamosBridgeMapper mapper;

  public ExternalLoansService(CjPrestamosClient client, CjPrestamosProperties properties, FinancialProfileRepository profileRepository, CjPrestamosBridgeMapper mapper) {
    this.client = client;
    this.properties = properties;
    this.profileRepository = profileRepository;
    this.mapper = mapper;
  }

  public ExternalLoansSummaryResponse getSummary(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) throw new ForbiddenException("Profile no pertenece al usuario");
    if (!properties.enabled()) return ExternalLoansSummaryResponse.disabled();
    return ExternalLoansSummaryResponse.enabled(
        mapper.toExternalDashboard(client.getDashboardSummary(profileId, userId)),
        mapper.toExternalCashControl(client.getCashControl(profileId, userId)),
        mapper.toExternalLoans(client.getActiveLoans(profileId, userId)));
  }
}
