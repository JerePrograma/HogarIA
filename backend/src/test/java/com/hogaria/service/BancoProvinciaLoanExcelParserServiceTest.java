package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class BancoProvinciaLoanExcelParserServiceTest {
  @Test void parseaEncabezadoMontosYFechas() throws Exception {
    var wb = new XSSFWorkbook(); var sh = wb.createSheet();
    sh.createRow(0).createCell(0).setCellValue("Banco de la Provincia de Buenos Aires - Mis Préstamos");
    var h = sh.createRow(1); h.createCell(0).setCellValue("Tipo"); h.createCell(1).setCellValue("Identificación"); h.createCell(2).setCellValue("Número de cuenta"); h.createCell(3).setCellValue("Deuda a la fecha"); h.createCell(4).setCellValue("Importe original"); h.createCell(5).setCellValue("Vencimiento");
    var r1 = sh.createRow(2); r1.createCell(0).setCellValue("Refinanciación de deuda"); r1.createCell(1).setCellValue("PRESTAMO"); r1.createCell(2).setCellValue("6176-688462/8"); r1.createCell(3).setCellValue("200.342,99"); r1.createCell(4).setCellValue("225.990,30"); r1.createCell(5).setCellValue("28/02/2027");
    var r2 = sh.createRow(3); r2.createCell(0).setCellValue("Refinanciación de deuda"); r2.createCell(1).setCellValue("PRESTAMO"); r2.createCell(2).setCellValue("6176-2009/5"); r2.createCell(3).setCellValue("2.935.649,69"); r2.createCell(4).setCellValue("3.951.577,14"); r2.createCell(5).setCellValue("31/07/2027");
    var out = new ByteArrayOutputStream(); wb.write(out); wb.close();
    var file = new MockMultipartFile("file", "prestamos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    var rows = new BancoProvinciaLoanExcelParserService().parse(file);
    assertEquals(2, rows.size());
    assertEquals("200342.99", rows.get(0).currentDebtAmount().toPlainString());
    assertEquals("2935649.69", rows.get(1).currentDebtAmount().toPlainString());
    assertEquals("2027-02-28", rows.get(0).dueDate().toString());
  }
}
