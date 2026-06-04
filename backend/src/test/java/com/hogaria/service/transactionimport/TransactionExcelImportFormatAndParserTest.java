package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.entity.MoneyTransaction;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionExcelImportFormatAndParserTest {

  private ImportTextNormalizer normalizer;
  private TransactionImportRuleClassifier classifier;
  private ImportSourceHashService hashService;
  private TransactionExcelImportFormatDetector detector;
  private UUID profileId;
  private UUID accountId;

  @BeforeEach
  void setup() {
    normalizer = new ImportTextNormalizer();
    classifier = new TransactionImportRuleClassifier(normalizer);
    hashService = new ImportSourceHashService();
    detector = new TransactionExcelImportFormatDetector(normalizer);
    profileId = UUID.randomUUID();
    accountId = UUID.randomUUID();
  }

  @Test
  void detectsMercadoPagoHeadersInFirstRow() throws Exception {
    var detection = detector.detect(mercadoPagoWorkbook(
            "2026-05-30T19:49:19.000-03:00",
            "op-uber",
            "Pago aprobado",
            "available_money",
            "Dinero disponible",
            "-1000.00",
            "true",
            "\"\"\"Uber\"\"\"",
            "",
            "",
            ""
    ));

    assertEquals(ExcelImportTemplate.MERCADO_PAGO_SETTLEMENT, detection.template());
    assertEquals(1, detection.headerRowNumber());
    assertEquals("sheet0", detection.sheetName());
  }

  @Test
  void detectsBancoProvinciaHeadersInSecondRow() throws Exception {
    var detection = detector.detect(bancoProvinciaWorkbook());

    assertEquals(ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS, detection.template());
    assertEquals(2, detection.headerRowNumber());
    assertEquals("Hoja 1", detection.sheetName());
  }

  @Test
  void parsesMercadoPagoSettlementNormalizesQuotesAndDates() throws Exception {
    var parser = new MercadoPagoSettlementExcelParser(normalizer, classifier, hashService, new ObjectMapper());
    var bytes = mercadoPagoWorkbook(
            "2026-06-04T01:00:00.000Z",
            "op-utc",
            "Pago aprobado",
            "available_money",
            "Dinero disponible",
            "-6483.00",
            "true",
            "\"\"\"Uber\"\"\"",
            "",
            "",
            ""
    );
    var detection = detector.detect(bytes);
    var rows = parser.parse(bytes, detection, profileId, accountId);

    assertEquals(1, rows.size());
    var row = rows.get(0);
    assertEquals("Uber", row.rawDescription());
    assertEquals("uber", row.normalizedDescription().substring(0, 4));
    assertEquals(LocalDate.of(2026, 6, 3), row.realDate());
    assertEquals(MoneyTransaction.OperationDateTimePrecision.DATE_TIME, row.operationDateTimePrecision());
    assertEquals(new BigDecimal("-6483.00"), row.signedAmount());
    assertEquals(new BigDecimal("6483.00"), row.amountAbs());
    assertEquals(MoneyTransaction.MovementType.EXPENSE, row.movementType());
    assertEquals("taxiyapps", row.categorySuggestionKey());
    assertEquals(MoneyTransaction.PaymentChannel.MERCADO_PAGO, row.paymentChannel());
    assertEquals(MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE, row.balanceImpact());
  }

  @Test
  void parsesMercadoPagoMercadoCreditoAndTechnicalNoise() throws Exception {
    var parser = new MercadoPagoSettlementExcelParser(normalizer, classifier, hashService, new ObjectMapper());
    var bytes = mercadoPagoWorkbookWithTwoRows();
    var detection = detector.detect(bytes);
    var rows = parser.parse(bytes, detection, profileId, accountId);

    assertEquals(2, rows.size());
    assertEquals(MoneyTransaction.PaymentChannel.MERCADO_CREDITO, rows.get(0).paymentChannel());
    assertEquals(MoneyTransaction.ClassificationStatus.CLASSIFIED, rows.get(0).classificationStatus());
    assertEquals("mercadocredito", rows.get(0).categorySuggestionKey());
    assertEquals(MoneyTransaction.ClassificationStatus.CLASSIFIED, rows.get(1).classificationStatus());
    assertEquals("rendimientomercadopago", rows.get(1).categorySuggestionKey());
    assertEquals(com.hogaria.dto.TransactionImportDtos.RowStatus.READY, rows.get(1).rowStatus());
  }

  @Test
  void parsesBancoProvinciaRowsWithChannelsCounterpartyAndCategories() throws Exception {
    var parser = new BancoProvinciaMovimientosExcelParser(normalizer, classifier, hashService, new ObjectMapper());
    var bytes = bancoProvinciaWorkbook();
    var detection = detector.detect(bytes);
    var rows = parser.parse(bytes, detection, profileId, accountId);

    assertEquals(4, rows.size());

    var salary = rows.get(0);
    assertEquals("9338", salary.sourceOperationId());
    assertEquals(MoneyTransaction.MovementType.INCOME, salary.movementType());
    assertEquals("sueldo", salary.categorySuggestionKey());
    assertEquals(MoneyTransaction.BalanceImpact.OPERATING_INCOME, salary.balanceImpact());

    var cuentaDni = rows.get(1);
    assertEquals(MoneyTransaction.PaymentChannel.CUENTA_DNI, cuentaDni.paymentChannel());
    assertEquals("MARIA LUJAN,BRAVO", cuentaDni.counterparty());
    assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, cuentaDni.classificationStatus());

    var uber = rows.get(2);
    assertEquals("PAYU*AR*UBER", uber.merchantName());
    assertEquals("taxiyapps", uber.categorySuggestionKey());
    assertEquals(MoneyTransaction.PaymentChannel.DEBIT_CARD, uber.paymentChannel());

    var interests = rows.get(3);
    assertEquals("interesesyrendimientos", interests.categorySuggestionKey());
    assertEquals(MoneyTransaction.BalanceImpact.INTEREST_INCOME, interests.balanceImpact());
  }

  private byte[] mercadoPagoWorkbook(
          String date,
          String operationId,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String amount,
          String liquidated,
          String detail,
          String identification,
          String orderId,
          String payer
  ) throws Exception {
    var wb = new XSSFWorkbook();
    var sh = wb.createSheet("sheet0");
    writeMercadoPagoHeader(sh.createRow(0));
    var row = sh.createRow(1);
    writeMercadoPagoRow(row, date, operationId, operationType, paymentMethodType, paymentMethod, amount, liquidated, detail, identification, orderId, payer);
    return toBytes(wb);
  }

  private byte[] mercadoPagoWorkbookWithTwoRows() throws Exception {
    var wb = new XSSFWorkbook();
    var sh = wb.createSheet("sheet0");
    writeMercadoPagoHeader(sh.createRow(0));
    writeMercadoPagoRow(
            sh.createRow(1),
            "2026-05-15T17:02:48.000-03:00",
            "158691275469",
            "CREDIT",
            "available_money",
            "Dinero disponible",
            "5399.00",
            "true",
            "\"\"\"Payment linked to a loan origination\"\"\"",
            "loan-1391138963",
            "",
            "MercadoCrédito"
    );
    writeMercadoPagoRow(
            sh.createRow(2),
            "2026-06-04T01:18:38.000-03:00",
            "1744754496462",
            "Pago aprobado",
            "",
            "",
            "0.12",
            "false",
            "",
            "",
            "",
            ""
    );
    return toBytes(wb);
  }

  private byte[] bancoProvinciaWorkbook() throws Exception {
    var wb = new XSSFWorkbook();
    var sh = wb.createSheet("Hoja 1");
    sh.createRow(0).createCell(0).setCellValue("Banco Provincia de Buenos Aires - Detalle de movimientos");
    var header = sh.createRow(1);
    var headers = new String[]{"Número Secuencia", "Fecha", "Importe", "Saldo", "Descripción", "Descripción Extendida", "Nombre Comercio"};
    for (int i = 0; i < headers.length; i++) {
      header.createCell(i).setCellValue(headers[i]);
    }

    writeBancoRow(sh.createRow(2), "9338", "30/01/2026", "28183", "777163.24", "CREDITO HABERES", "Policia 30648839088 BCO.014 REF.565068418263545", "");
    writeBancoRow(sh.createRow(3), "9336", "30/01/2026", "-10000", "299214.42", "DEBITO CUENTA DNI", "CDNI 30/01-C.703925 D:27403079702 N:MARIA LUJAN,BRAVO", "");
    writeBancoRow(sh.createRow(4), "9332", "30/01/2026", "-5378", "325708.03", "PAGO CON TARJETA DEBITO", "COMPRA TARJETA 29/01/26 23:43 COMPR. 000602902167350", "PAYU*AR*UBER");
    writeBancoRow(sh.createRow(5), "9320", "28/01/2026", "17.01", "76944.03", "ACREDITACION INTERESES", "ACREDIT. INTERESES PERIODO DESDE 26-12-2025 HASTA 27-01-2026", "");
    return toBytes(wb);
  }

  private void writeMercadoPagoHeader(org.apache.poi.ss.usermodel.Row row) {
    var headers = new String[]{
            "FECHA DE ORIGEN",
            "FECHA DE APROBACIÓN",
            "ID DE OPERACIÓN EN MERCADO PAGO",
            "TIPO DE OPERACIÓN",
            "TIPO DE MEDIO DE PAGO",
            "MEDIO DE PAGO",
            "VALOR DE LA COMPRA",
            "COMISIONES + IVA",
            "MONTO NETO DE LA OPERACIÓN",
            "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO",
            "MONEDA",
            "LIQUIDADO",
            "DETALLE DE LA VENTA",
            "NÚMERO DE IDENTIFICACIÓN",
            "ID DE LA ORDEN",
            "PAGADOR"
    };
    for (int i = 0; i < headers.length; i++) {
      row.createCell(i).setCellValue(headers[i]);
    }
  }

  private void writeMercadoPagoRow(
          org.apache.poi.ss.usermodel.Row row,
          String date,
          String operationId,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String amount,
          String liquidated,
          String detail,
          String identification,
          String orderId,
          String payer
  ) {
    var values = new String[]{
            date,
            date,
            operationId,
            operationType,
            paymentMethodType,
            paymentMethod,
            amount,
            "0.00",
            amount,
            amount,
            "ARS",
            liquidated,
            detail,
            identification,
            orderId,
            payer
    };
    for (int i = 0; i < values.length; i++) {
      row.createCell(i).setCellValue(values[i]);
    }
  }

  private void writeBancoRow(
          org.apache.poi.ss.usermodel.Row row,
          String sequence,
          String date,
          String amount,
          String balance,
          String description,
          String extendedDescription,
          String merchantName
  ) {
    var values = new String[]{sequence, date, amount, balance, description, extendedDescription, merchantName};
    for (int i = 0; i < values.length; i++) {
      row.createCell(i).setCellValue(values[i]);
    }
  }

  private byte[] toBytes(XSSFWorkbook workbook) throws Exception {
    try (workbook) {
      var out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    }
  }
}
