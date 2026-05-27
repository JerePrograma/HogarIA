package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hogaria.dto.AccountDtos.AccountCreateRequest;
import com.hogaria.dto.CategoryDtos.CategoryCreateRequest;
import com.hogaria.entity.Account;
import com.hogaria.entity.Category;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryAccountNormalizationTest {

    @Mock AccountRepository accountRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock FinancialProfileRepository profileRepository;

    AccountKeyNormalizer accountKeyNormalizer = new AccountKeyNormalizer();
    CategoryKeyNormalizer categoryKeyNormalizer = new CategoryKeyNormalizer();

    @Test
    void equivalentCategoryNamesShareKeyAndCannotDuplicateInSameProfileType() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var service = new CategoryService(categoryRepository, profileRepository, categoryKeyNormalizer);

        assertEquals("mercadopago", categoryKeyNormalizer.normalize("Mercado Pago"));
        assertEquals("mercadopago", categoryKeyNormalizer.normalize("mercado-pago"));

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(categoryRepository.existsActiveProfileDuplicateKey(profileId, "mercadopago", Category.Type.VARIABLE_EXPENSE, null))
                .thenReturn(true);

        assertThrows(
                DomainConflictException.class,
                () -> service.create(userId, profileId, new CategoryCreateRequest(null, "Mercado-Pago", Category.Type.VARIABLE_EXPENSE, Category.Scope.PERSONAL))
        );
    }

    @Test
    void equivalentAccountNamesShareKeyAndCannotDuplicateInSameCurrency() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var service = new AccountService(accountRepository, profileRepository, accountKeyNormalizer);

        assertEquals("mercadopago", accountKeyNormalizer.normalize("Mercado Pago"));
        assertEquals("mercadopago", accountKeyNormalizer.normalize("MERCADO-PAGO"));

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(accountRepository.existsActiveDuplicateKey(profileId, "mercadopago", "ARS", null)).thenReturn(true);

        assertThrows(
                DomainConflictException.class,
                () -> service.create(userId, profileId, new AccountCreateRequest("Mercado Pago", Account.AccountType.VIRTUAL_WALLET, "ARS", null, null, null))
        );
    }

    @Test
    void newNormalizedKeysAreStoredOnCreate() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountService = new AccountService(accountRepository, profileRepository, accountKeyNormalizer);
        var categoryService = new CategoryService(categoryRepository, profileRepository, categoryKeyNormalizer);

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var account = accountService.create(userId, profileId, new AccountCreateRequest("Mercado Pago", Account.AccountType.VIRTUAL_WALLET, "ARS", null, null, null));
        var category = categoryService.create(userId, profileId, new CategoryCreateRequest(null, "Mercado Pago", Category.Type.VARIABLE_EXPENSE, Category.Scope.PERSONAL));

        assertEquals("mercadopago", account.accountKey());
        assertEquals("mercadopago", category.categoryKey());
    }
}
