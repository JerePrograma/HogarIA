package com.hogaria.service;

import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExternalLoansService {
  private final CjPrestamosClient client;
  private final CjPrestamosProperties properties;
  private final FinancialProfileRepository profileRepository;

  public ExternalLoansService(CjPrestamosClient client, CjPrestamosProperties properties, FinancialProfileRepository profileRepository) {
    this.client = client;
    this.properties = properties;
    this.profileRepository = profileRepository;
  }

  public ExternalLoansSummaryResponse getSummary(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) throw new ForbiddenException("Profile no pertenece al usuario");
    if (!properties.enabled()) return ExternalLoansSummaryResponse.disabled();
    return ExternalLoansSummaryResponse.enabled(
        client.getDashboardSummary(profileId, userId),
        client.getCashControl(profileId, userId),
        client.getActiveLoans(profileId, userId));
  }
}
