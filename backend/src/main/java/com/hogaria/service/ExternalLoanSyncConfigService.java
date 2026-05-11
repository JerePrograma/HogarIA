package com.hogaria.service;

import com.hogaria.dto.ExternalLoanSyncConfigDtos.*;
import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.*;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExternalLoanSyncConfigService {
  private final ExternalLoanSyncConfigRepository repository;
  private final FinancialProfileRepository profileRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;

  public ExternalLoanSyncConfigService(ExternalLoanSyncConfigRepository repository, FinancialProfileRepository profileRepository, AccountRepository accountRepository, CategoryRepository categoryRepository) {
    this.repository = repository;
    this.profileRepository = profileRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
  }

  public ExternalLoanSyncConfigResponse get(UUID userId, UUID profileId) {
    ensureProfile(userId, profileId);
    return repository.findByProfileId(profileId).map(this::toResponse).orElse(null);
  }

  public ExternalLoanSyncConfigResponse upsert(UUID userId, UUID profileId, ExternalLoanSyncConfigUpsertRequest request) {
    ensureProfile(userId, profileId);
    if (Boolean.TRUE.equals(request.enabled()) && hasMissingReferences(request)) {
      throw new BadRequestException("No se puede habilitar sin todas las referencias configuradas");
    }

    validateAccount(profileId, request.accountId());
    validateCategory(profileId, request.loanDisbursementCategoryId());
    validateCategory(profileId, request.principalRecoveryCategoryId());
    validateCategory(profileId, request.interestIncomeCategoryId());

    var config = repository.findByProfileId(profileId).orElseGet(() -> ExternalLoanSyncConfig.builder().profileId(profileId).build());
    config.setAccountId(request.accountId());
    config.setLoanDisbursementCategoryId(request.loanDisbursementCategoryId());
    config.setPrincipalRecoveryCategoryId(request.principalRecoveryCategoryId());
    config.setInterestIncomeCategoryId(request.interestIncomeCategoryId());
    config.setEnabled(request.enabled());
    return toResponse(repository.save(config));
  }

  private boolean hasMissingReferences(ExternalLoanSyncConfigUpsertRequest request) {
    return request.accountId() == null
        || request.loanDisbursementCategoryId() == null
        || request.principalRecoveryCategoryId() == null
        || request.interestIncomeCategoryId() == null;
  }

  private void ensureProfile(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) throw new ForbiddenException("Profile no pertenece al usuario");
  }

  private void validateAccount(UUID profileId, UUID accountId) {
    if (accountId == null) return;
    if (!accountRepository.existsByIdAndProfileId(accountId, profileId)) throw new BadRequestException("La cuenta no pertenece al perfil");
  }

  private void validateCategory(UUID profileId, UUID categoryId) {
    if (categoryId == null) return;
    Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new BadRequestException("Categoria no encontrada"));
    if (!Objects.equals(category.getProfileId(), profileId) && category.getProfileId() != null) {
      throw new BadRequestException("La categoria debe pertenecer al perfil o ser global");
    }
  }

  private ExternalLoanSyncConfigResponse toResponse(ExternalLoanSyncConfig config) {
    return new ExternalLoanSyncConfigResponse(
        config.getId(), config.getProfileId(), config.getAccountId(), config.getLoanDisbursementCategoryId(),
        config.getPrincipalRecoveryCategoryId(), config.getInterestIncomeCategoryId(), config.getEnabled(),
        config.getCreatedAt(), config.getUpdatedAt());
  }
}
