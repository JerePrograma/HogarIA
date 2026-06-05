package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RealTransactionImportFilesVerificationTest {

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BANCO_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MP_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String OWN_ID = "20384307400";

    private static Stream<ExpectedFile> files() {
        return Stream.of(
                new ExpectedFile("MercadoPago Enero.xlsx", TransactionImportSource.MERCADO_PAGO, 25, "217162.36", "128053.27"),
                new ExpectedFile("MercadoPago Febrero.xlsx", TransactionImportSource.MERCADO_PAGO, 13, "655273.86", "19001.86"),
                new ExpectedFile("MercadoPago Marzo.xlsx", TransactionImportSource.MERCADO_PAGO, 32, "374333.68", "237579.64"),
                new ExpectedFile("MercadoPago Abril.xlsx", TransactionImportSource.MERCADO_PAGO, 41, "285897.16", "215263.89"),
                new ExpectedFile("MercadoPago Mayo.xlsx", TransactionImportSource.MERCADO_PAGO, 61, "1051281.04", "376933.11"),
                new ExpectedFile("Provincia Enero.xlsx", TransactionImportSource.BANCO_PROVINCIA, 187, "3824866.08", "3503925.32"),
                new ExpectedFile("Provincia Febrero.xlsx", TransactionImportSource.BANCO_PROVINCIA, 121, "3607761.96", "3898713.85"),
                new ExpectedFile("Provincia Marzo.xlsx", TransactionImportSource.BANCO_PROVINCIA, 152, "3522556.16", "3794645.02"),
                new ExpectedFile("Provincia Abril.xlsx", TransactionImportSource.BANCO_PROVINCIA, 125, "3456162.98", "3454051.60"),
                new ExpectedFile("Provincia Mayo.xlsx", TransactionImportSource.BANCO_PROVINCIA, 150, "5880654.39", "5190433.21")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("files")
    void parsesExactRowsAndGrossTotals(ExpectedFile expected) throws Exception {
        var parsed = parse(expected);

        assertEquals(expected.rows(), parsed.candidates().size());
        assertAmount(expected.positive(), sumPositive(parsed.candidates()));
        assertAmount(expected.negative(), sumNegative(parsed.candidates()));
        assertEquals(0, parsed.candidates().stream().filter(row -> row.rowStatus() == RowStatus.ERROR).count());
        assertTrue(parsed.candidates().stream().allMatch(row -> row.sourceHash() != null && !row.sourceHash().isBlank()));
    }

    @Test
    void validatesAllRealFilesClassificationMatchingAndFinalReport() throws Exception {
        var parsedFiles = files().map(expected -> {
            try {
                return parse(expected);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }).toList();
        var candidates = parsedFiles.stream().flatMap(file -> file.candidates().stream()).toList();

        assertEquals(907, candidates.size());

        var snapshots = toSnapshots(parsedFiles);
        var result = new ImportInternalTransferMatcher().match(snapshots, Set.of(OWN_ID));
        var strongMatches = result.matches().stream().filter(ImportInternalTransferMatcher.MatchedPair::strong).toList();

        assertEquals(20, strongMatches.size());
        assertAmount("1512952.00", strongMatches.stream()
                .map(ImportInternalTransferMatcher.MatchedPair::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(40, result.rows().stream()
                .filter(snapshot -> snapshot.row().status() == RowStatus.INTERNAL_TRANSFER_MATCHED)
                .count());

        assertUnmatchedMpFunding(result, LocalDate.of(2026, 2, 7), "10000.00");
        assertUnmatchedMpFunding(result, LocalDate.of(2026, 5, 12), "2000.00");
        assertUnmatchedMpFunding(result, LocalDate.of(2026, 5, 12), "15000.00");

        assertTrue(result.rows().stream()
                .filter(snapshot -> snapshot.row().status() == RowStatus.INTERNAL_TRANSFER_MATCHED)
                .filter(snapshot -> snapshot.row().source() == TransactionImportSource.BANCO_PROVINCIA)
                .allMatch(snapshot -> text(snapshot.row().extendedDescription()).contains(OWN_ID)));
        assertTrue(result.rows().stream()
                .filter(snapshot -> text(snapshot.row().rawDescription()).contains("DEBITO CUENTA DNI"))
                .noneMatch(snapshot -> snapshot.row().status() == RowStatus.INTERNAL_TRANSFER_MATCHED));

        var unmatchedOwnDebin = find(result.rows(), TransactionImportSource.BANCO_PROVINCIA, "37300.00",
                row -> text(row.extendedDescription()).contains(OWN_ID));
        assertEquals(RowStatus.POSSIBLE_INTERNAL_TRANSFER, unmatchedOwnDebin.status());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, unmatchedOwnDebin.balanceImpact());

        assertRealClassificationCases(candidates);
        assertRepeatedMercadoPagoOperationIdIsNotDeduplicated(candidates);
        printReport(parsedFiles, result);
    }

    private void assertRealClassificationCases(List<ImportedMovementCandidate> candidates) {
        var loanOrigination = candidates.stream()
                .filter(row -> text(row.rawDescription()).contains("PAYMENT LINKED TO A LOAN ORIGINATION"))
                .findFirst().orElseThrow();
        assertEquals(MoneyTransaction.BalanceImpact.LOAN_ORIGINATION, loanOrigination.balanceImpact());
        assertNotEquals(MoneyTransaction.BalanceImpact.OPERATING_INCOME, loanOrigination.balanceImpact());

        var mercadoCreditoPayment = candidates.stream()
                .filter(row -> text(row.rawDescription()).contains("PAGO DE CREDITOS DE MERCADO PAGO")
                        || text(row.rawDescription()).contains("PAGO DE CUOTAS DE MERCADO CREDITO"))
                .findFirst().orElseThrow();
        assertEquals(MoneyTransaction.BalanceImpact.DEBT_OUTFLOW, mercadoCreditoPayment.balanceImpact());

        var variosMegumi = candidates.stream()
                .filter(row -> row.signedAmount() != null && row.signedAmount().signum() < 0)
                .filter(row -> text(row.rawDescription()).contains("VARIOS"))
                .filter(row -> text(row.extendedDescription()).contains("MEGUMI"))
                .findFirst().orElseThrow();
        assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, variosMegumi.classificationStatus());
        assertNotEquals(MoneyTransaction.ClassificationStatus.TECHNICAL, variosMegumi.classificationStatus());
        assertEquals(MoneyTransaction.MovementType.EXPENSE, variosMegumi.movementType());

        var paymentLink = candidates.stream()
                .filter(row -> text(row.rawDescription()).contains("LINK DE PAGO"))
                .findFirst().orElseThrow();
        assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, paymentLink.classificationStatus());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, paymentLink.balanceImpact());

        var yield = candidates.stream()
                .filter(row -> row.source() == TransactionImportSource.MERCADO_PAGO)
                .filter(row -> row.rawDescription() == null || row.rawDescription().isBlank())
                .filter(row -> row.signedAmount() != null && row.signedAmount().signum() > 0)
                .filter(row -> row.signedAmount().compareTo(new BigDecimal("100.00")) < 0)
                .findFirst().orElseThrow();
        assertEquals(MoneyTransaction.BalanceImpact.INTEREST_INCOME, yield.balanceImpact());

        assertEquals(MoneyTransaction.BalanceImpact.OPERATING_INCOME, firstBanco(candidates, "SYSTEMSCORP").balanceImpact());
        assertEquals(MoneyTransaction.BalanceImpact.OPERATING_INCOME, firstBanco(candidates, "CREDITO HABERES").balanceImpact());

        var arca = firstBanco(candidates, "ARCA");
        assertEquals(MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE, arca.balanceImpact());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, arca.balanceImpact());

        assertEquals(MoneyTransaction.BalanceImpact.REFUND_OR_REIMBURSEMENT, firstBanco(candidates, "BENEF PEI-CUENTA DNI").balanceImpact());
        assertEquals(MoneyTransaction.BalanceImpact.DEBT_OUTFLOW, firstBanco(candidates, "PAGO CUOTA DE PRESTAMO").balanceImpact());

        var cdniPurchase = firstBanco(candidates, "PAGO CON TRANSFERENCIA CDNI");
        assertEquals(MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE, cdniPurchase.balanceImpact());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, cdniPurchase.balanceImpact());

        var externalCuentaDni = firstBanco(candidates, "DEBITO CUENTA DNI");
        assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, externalCuentaDni.classificationStatus());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, externalCuentaDni.balanceImpact());
    }

    private void assertRepeatedMercadoPagoOperationIdIsNotDeduplicated(List<ImportedMovementCandidate> candidates) {
        var repeated = candidates.stream()
                .filter(row -> "1744344685519".equals(row.sourceOperationId()))
                .toList();
        assertEquals(2, repeated.size());
        assertEquals(Set.of(new BigDecimal("0.01"), new BigDecimal("0.60")),
                repeated.stream().map(ImportedMovementCandidate::signedAmount).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, repeated.stream().map(ImportedMovementCandidate::sourceHash).distinct().count());
    }

    private void printReport(
            List<ParsedFile> parsedFiles,
            ImportInternalTransferMatcher.MatchResult result
    ) {
        var byBatch = result.rows().stream().collect(java.util.stream.Collectors.groupingBy(
                ImportInternalTransferMatcher.ImportRowSnapshot::batchId
        ));
        var hashCounts = result.rows().stream()
                .map(snapshot -> snapshot.row().sourceHash())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(value -> value, java.util.stream.Collectors.counting()));

        for (var file : parsedFiles) {
            var rows = byBatch.getOrDefault(file.batchId(), List.of()).stream()
                    .map(ImportInternalTransferMatcher.ImportRowSnapshot::row)
                    .toList();
            long confirmed = rows.stream().filter(row -> row.status() == RowStatus.INTERNAL_TRANSFER_MATCHED).count();
            long possible = rows.stream().filter(row -> row.status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER).count();
            long review = rows.stream().filter(row -> row.classificationStatus() == MoneyTransaction.ClassificationStatus.REVIEW).count();
            long errors = rows.stream().filter(row -> row.status() == RowStatus.ERROR).count();
            long duplicates = rows.stream().filter(row -> row.sourceHash() != null && hashCounts.getOrDefault(row.sourceHash(), 0L) > 1).count();
            System.out.printf(
                    "REAL_IMPORT_REPORT|%s|%d|%s|%s|%d|%d|%d|%d|%d%n",
                    file.expected().fileName(),
                    rows.size(),
                    sumPositive(file.candidates()),
                    sumNegative(file.candidates()),
                    confirmed,
                    possible,
                    review,
                    errors,
                    duplicates
            );
        }
    }

    private List<ImportInternalTransferMatcher.ImportRowSnapshot> toSnapshots(List<ParsedFile> parsedFiles) {
        var categories = org.mockito.Mockito.mock(CategoryRepository.class);
        org.mockito.Mockito.when(categories.findByProfileIdAndActiveTrue(PROFILE_ID)).thenReturn(List.of());
        org.mockito.Mockito.when(categories.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of());
        var mapper = new TransactionImportPreviewMapper(categories, new TransactionImportCategoryResolver(categories));
        var snapshots = new ArrayList<ImportInternalTransferMatcher.ImportRowSnapshot>();
        for (var file : parsedFiles) {
            for (var candidate : file.candidates()) {
                snapshots.add(new ImportInternalTransferMatcher.ImportRowSnapshot(
                        UUID.randomUUID(),
                        file.batchId(),
                        mapper.toPreviewRow(PROFILE_ID, candidate)
                ));
            }
        }
        return snapshots;
    }

    private void assertUnmatchedMpFunding(
            ImportInternalTransferMatcher.MatchResult result,
            LocalDate date,
            String amount
    ) {
        var row = find(result.rows(), TransactionImportSource.MERCADO_PAGO, amount,
                candidate -> date.equals(candidate.realDate())
                        && (text(candidate.rawDescription()).contains("PAGO DEBIN")
                        || text(candidate.rawDescription()).contains("BANK TRANSFER")));
        assertNotEquals(RowStatus.INTERNAL_TRANSFER_MATCHED, row.status());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, row.balanceImpact());
    }

    private TransactionImportPreviewRow find(
            List<ImportInternalTransferMatcher.ImportRowSnapshot> rows,
            TransactionImportSource source,
            String amount,
            java.util.function.Predicate<TransactionImportPreviewRow> predicate
    ) {
        return rows.stream()
                .map(ImportInternalTransferMatcher.ImportRowSnapshot::row)
                .filter(row -> row.source() == source)
                .filter(row -> row.amount() != null && row.amount().compareTo(new BigDecimal(amount)) == 0)
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }

    private ImportedMovementCandidate firstBanco(List<ImportedMovementCandidate> candidates, String text) {
        var expected = text(text);
        return candidates.stream()
                .filter(row -> row.source() == TransactionImportSource.BANCO_PROVINCIA)
                .filter(row -> text(row.rawDescription()).contains(expected)
                        || text(row.extendedDescription()).contains(expected))
                .findFirst()
                .orElseThrow();
    }

    private ParsedFile parse(ExpectedFile expected) throws Exception {
        var path = realFilesDirectory().resolve(expected.fileName());
        Assumptions.assumeTrue(Files.isRegularFile(path), "Missing real file: " + path);
        var bytes = Files.readAllBytes(path);
        var normalizer = new ImportTextNormalizer();
        var classifier = new TransactionImportRuleClassifier(normalizer);
        var hashService = new ImportSourceHashService();
        var objectMapper = new ObjectMapper().findAndRegisterModules();
        var detection = new TransactionExcelImportFormatDetector(normalizer).detect(bytes);
        TransactionExcelMovementParser parser = expected.source() == TransactionImportSource.BANCO_PROVINCIA
                ? new BancoProvinciaMovimientosExcelParser(normalizer, classifier, hashService, objectMapper)
                : new MercadoPagoSettlementExcelParser(normalizer, classifier, hashService, objectMapper);
        var accountId = expected.source() == TransactionImportSource.BANCO_PROVINCIA ? BANCO_ACCOUNT_ID : MP_ACCOUNT_ID;
        return new ParsedFile(expected, UUID.randomUUID(), parser.parse(bytes, detection, PROFILE_ID, accountId));
    }

    private Path realFilesDirectory() {
        var configured = System.getProperty("transactionImportRealFilesDir", "");
        if (!configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.home"), "Downloads", "Seguimiento bancario");
    }

    private BigDecimal sumPositive(List<ImportedMovementCandidate> rows) {
        return rows.stream()
                .map(ImportedMovementCandidate::signedAmount)
                .filter(Objects::nonNull)
                .filter(amount -> amount.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumNegative(List<ImportedMovementCandidate> rows) {
        return rows.stream()
                .map(ImportedMovementCandidate::signedAmount)
                .filter(Objects::nonNull)
                .filter(amount -> amount.signum() < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "amount");
    }

    private String text(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private record ExpectedFile(
            String fileName,
            TransactionImportSource source,
            int rows,
            String positive,
            String negative
    ) {
        @Override
        public String toString() {
            return fileName;
        }
    }

    private record ParsedFile(ExpectedFile expected, UUID batchId, List<ImportedMovementCandidate> candidates) {
    }
}
