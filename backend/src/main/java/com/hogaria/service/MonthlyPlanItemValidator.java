package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

class MonthlyPlanItemValidator {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    MonthlyPlanItemValidator(
            CategoryRepository categoryRepository,
            AccountRepository accountRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    void validate(
            MonthlyPlanItem item,
            Set<MonthlyPlanItem.Type> allowedTypes,
            boolean requireAmountOrRange
    ) {
        if (item.getType() == null) {
            throw new BadRequestException("Tipo requerido");
        }

        if (allowedTypes != null && !allowedTypes.contains(item.getType())) {
            throw new BadRequestException("Tipo no permitido");
        }

        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Título requerido");
        }

        item.setTitle(item.getTitle().trim());

        if (item.getPeriodYear() == null || item.getPeriodYear() < 2000 || item.getPeriodYear() > 2100) {
            throw new BadRequestException("Año inválido");
        }

        if (item.getPeriodMonth() == null || item.getPeriodMonth() < 1 || item.getPeriodMonth() > 12) {
            throw new BadRequestException("Mes inválido");
        }

        if (requireAmountOrRange
                && item.getAmount() == null
                && item.getMinAmount() == null
                && item.getMaxAmount() == null) {
            throw new BadRequestException("Debe tener monto o rango");
        }

        for (var amount : new BigDecimal[]{
                item.getAmount(),
                item.getMinAmount(),
                item.getMaxAmount(),
                item.getExpectedRecoveryAmount()
        }) {
            if (amount != null && amount.signum() < 0) {
                throw new BadRequestException("Los montos no pueden ser negativos");
            }
        }

        if (item.getMinAmount() != null
                && item.getMaxAmount() != null
                && item.getMinAmount().compareTo(item.getMaxAmount()) > 0) {
            throw new BadRequestException("Mínimo no puede superar máximo");
        }

        if (item.getExpectedRecoveryPercent() != null
                && (item.getExpectedRecoveryPercent().signum() < 0
                || item.getExpectedRecoveryPercent().compareTo(new BigDecimal("100")) > 0)) {
            throw new BadRequestException("Recupero % inválido");
        }

        if (item.getInstallmentNumber() != null && item.getInstallmentNumber() <= 0) {
            throw new BadRequestException("Número de cuota inválido");
        }

        if (item.getInstallmentTotal() != null && item.getInstallmentTotal() <= 0) {
            throw new BadRequestException("Total de cuotas inválido");
        }

        if (item.getInstallmentNumber() != null
                && item.getInstallmentTotal() != null
                && item.getInstallmentNumber() > item.getInstallmentTotal()) {
            throw new BadRequestException("Cuota inválida");
        }

        if (item.getCurrency() == null || !item.getCurrency().trim().matches("[A-Za-z]{3}")) {
            throw new BadRequestException("Moneda inválida");
        }

        item.setCurrency(item.getCurrency().trim().toUpperCase(Locale.ROOT));

        validateExpectedDateNearOperationalPeriod(item);
    }

    void validateReferences(
            UUID profileId,
            UUID accountId,
            UUID categoryId,
            boolean requireActiveCategory,
            boolean rejectTechnicalCategory
    ) {
        if (accountId != null && !accountRepository.existsByIdAndProfileId(accountId, profileId)) {
            throw new BadRequestException("Cuenta inválida para perfil");
        }

        if (categoryId == null) {
            return;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

        if (category.getProfileId() != null && !Objects.equals(category.getProfileId(), profileId)) {
            throw new ForbiddenException("Categoría inválida para perfil");
        }

        if (requireActiveCategory && Boolean.FALSE.equals(category.getActive())) {
            throw new BadRequestException("Categoría inactiva");
        }

        if (rejectTechnicalCategory && Boolean.TRUE.equals(category.getTechnical())) {
            throw new BadRequestException("Categoría técnica no permitida");
        }
    }

    private void validateExpectedDateNearOperationalPeriod(MonthlyPlanItem item) {
        if (item.getExpectedDate() == null) {
            return;
        }

        YearMonth expectedPeriod = YearMonth.from(item.getExpectedDate());
        YearMonth itemPeriod = YearMonth.of(item.getPeriodYear(), item.getPeriodMonth());

        YearMonth minAllowedPeriod = itemPeriod.minusMonths(1);
        YearMonth maxAllowedPeriod = itemPeriod.plusMonths(1);

        if (expectedPeriod.isBefore(minAllowedPeriod) || expectedPeriod.isAfter(maxAllowedPeriod)) {
            throw new BadRequestException("Fecha esperada demasiado alejada del período");
        }
    }
}
