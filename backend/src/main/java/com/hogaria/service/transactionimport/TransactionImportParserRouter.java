package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.exception.BadRequestException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionImportParserRouter {

    private final TransactionExcelImportFormatDetector excelFormatDetector;
    private final List<TransactionExcelMovementParser> excelMovementParsers;
    private final TransactionImportPreviewMapper previewMapper;
    private final MercadoPagoDelimitedImportParser mercadoPagoDelimitedImportParser;
    private final GenericCardImportParser genericCardImportParser;

    public TransactionImportParserRouter(
            TransactionExcelImportFormatDetector excelFormatDetector,
            List<TransactionExcelMovementParser> excelMovementParsers,
            TransactionImportPreviewMapper previewMapper,
            MercadoPagoDelimitedImportParser mercadoPagoDelimitedImportParser,
            GenericCardImportParser genericCardImportParser
    ) {
        this.excelFormatDetector = excelFormatDetector;
        this.excelMovementParsers = excelMovementParsers;
        this.previewMapper = previewMapper;
        this.mercadoPagoDelimitedImportParser = mercadoPagoDelimitedImportParser;
        this.genericCardImportParser = genericCardImportParser;
    }

    public List<TransactionImportPreviewRow> parse(
            TransactionImportSource source,
            MultipartFile file,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo de importación está vacío.");
        }

        try {
            return switch (source) {
                case AUTO -> parseDetectedExcel(file.getBytes(), profileId, accountId, null);
                case BANCO_PROVINCIA -> parseDetectedExcel(
                        file.getBytes(),
                        profileId,
                        accountId,
                        ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS
                );
                case MERCADO_PAGO -> parseMercadoPago(file, profileId, accountId, year, month);
                case TARJETA_CREDITO_GENERICA -> genericCardImportParser.parse(
                        file,
                        profileId,
                        accountId,
                        year,
                        month,
                        false
                );
                case DEUDAS_TARJETA_GENERICA -> genericCardImportParser.parse(
                        file,
                        profileId,
                        accountId,
                        year,
                        month,
                        true
                );
            };
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Cannot parse file: " + ex.getMessage());
        }
    }

    private List<TransactionImportPreviewRow> parseMercadoPago(
            MultipartFile file,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month
    ) throws Exception {
        var bytes = file.getBytes();

        if (ImportFileTypeDetector.looksLikeExcelFile(bytes, file.getOriginalFilename())) {
            return parseDetectedExcel(
                    bytes,
                    profileId,
                    accountId,
                    ExcelImportTemplate.MERCADO_PAGO_SETTLEMENT
            );
        }

        return mercadoPagoDelimitedImportParser.parse(bytes, profileId, accountId, year, month);
    }

    private List<TransactionImportPreviewRow> parseDetectedExcel(
            byte[] bytes,
            UUID profileId,
            UUID accountId,
            ExcelImportTemplate expectedTemplate
    ) {
        var detection = excelFormatDetector.detect(bytes);

        if (expectedTemplate != null && detection.template() != expectedTemplate) {
            throw new BadRequestException(
                    "El archivo detectado es " + detection.template().name()
                            + ", pero se esperaba " + expectedTemplate.name()
                            + ". Headers detectados en fila " + detection.headerRowNumber()
                            + ": " + String.join(" | ", detection.headers())
            );
        }

        var parser = excelMovementParsers
                .stream()
                .filter(candidate -> candidate.template() == detection.template())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No hay parser registrado para " + detection.template().name()));

        return parser
                .parse(bytes, detection, profileId, accountId)
                .stream()
                .map(candidate -> previewMapper.toPreviewRow(profileId, candidate))
                .toList();
    }
}