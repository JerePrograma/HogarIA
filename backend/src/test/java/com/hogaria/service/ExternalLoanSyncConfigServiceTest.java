package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.dto.ExternalLoanSyncConfigDtos.ExternalLoanSyncConfigUpsertRequest;
import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalLoanSyncConfigServiceTest {
  @Mock ExternalLoanSyncConfigRepository repository;
  @Mock FinancialProfileRepository profileRepository;
  @Mock AccountRepository accountRepository;
  @Mock CategoryRepository categoryRepository;

  ExternalLoanSyncConfigService service;

  @BeforeEach void setUp() {
    service = new ExternalLoanSyncConfigService(repository, profileRepository, accountRepository, categoryRepository);
  }

  @Test void throwsWhenProfileNotOwned() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(false);
    assertThrows(ForbiddenException.class, () -> service.get(userId, profileId));
  }

  @Test void upsertValidatesAccountAndCategories() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var req = new ExternalLoanSyncConfigUpsertRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(accountRepository.existsByIdAndProfileId(req.accountId(), profileId)).thenReturn(false);

    assertThrows(BadRequestException.class, () -> service.upsert(userId, profileId, req));
  }

  @Test void upsertAllowsProfileAndGlobalCategories() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var req = new ExternalLoanSyncConfigUpsertRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(accountRepository.existsByIdAndProfileId(req.accountId(), profileId)).thenReturn(true);
    when(categoryRepository.findById(req.loanDisbursementCategoryId())).thenReturn(Optional.of(Category.builder().id(req.loanDisbursementCategoryId()).profileId(profileId).build()));
    when(categoryRepository.findById(req.principalRecoveryCategoryId())).thenReturn(Optional.of(Category.builder().id(req.principalRecoveryCategoryId()).profileId(null).build()));
    when(categoryRepository.findById(req.interestIncomeCategoryId())).thenReturn(Optional.of(Category.builder().id(req.interestIncomeCategoryId()).profileId(profileId).build()));
    when(repository.findByProfileId(profileId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(i -> {
      ExternalLoanSyncConfig c = i.getArgument(0);
      c.setId(UUID.randomUUID());
      return c;
    });

    var response = service.upsert(userId, profileId, req);

    assertTrue(response.enabled());
    assertEquals(req.accountId(), response.accountId());
  }

  @Test void rejectsCategoryFromAnotherProfile() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var req = new ExternalLoanSyncConfigUpsertRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), false);
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(accountRepository.existsByIdAndProfileId(req.accountId(), profileId)).thenReturn(true);
    when(categoryRepository.findById(req.loanDisbursementCategoryId())).thenReturn(Optional.of(Category.builder().id(req.loanDisbursementCategoryId()).profileId(UUID.randomUUID()).build()));

    assertThrows(BadRequestException.class, () -> service.upsert(userId, profileId, req));
  }


  @Test void upsertDisabledAllowsNullReferences() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var req = new ExternalLoanSyncConfigUpsertRequest(null, null, null, null, false);
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(repository.findByProfileId(profileId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    var response = service.upsert(userId, profileId, req);

    assertFalse(response.enabled());
    assertNull(response.accountId());
    assertNull(response.loanDisbursementCategoryId());
    assertNull(response.principalRecoveryCategoryId());
    assertNull(response.interestIncomeCategoryId());
    verifyNoInteractions(accountRepository, categoryRepository);
  }

  @Test void upsertEnabledRequiresAllReferences() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var req = new ExternalLoanSyncConfigUpsertRequest(null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);

    var ex = assertThrows(BadRequestException.class, () -> service.upsert(userId, profileId, req));
    assertTrue(ex.getMessage().contains("accountId"));
    verifyNoInteractions(accountRepository, categoryRepository, repository);
  }

}
