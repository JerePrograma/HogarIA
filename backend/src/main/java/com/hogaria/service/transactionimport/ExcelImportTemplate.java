package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;

public enum ExcelImportTemplate {
  MERCADO_PAGO_SETTLEMENT(TransactionImportSource.MERCADO_PAGO),
  BANCO_PROVINCIA_MOVIMIENTOS(TransactionImportSource.BANCO_PROVINCIA);

  private final TransactionImportSource source;

  ExcelImportTemplate(TransactionImportSource source) {
    this.source = source;
  }

  public TransactionImportSource source() {
    return source;
  }
}
