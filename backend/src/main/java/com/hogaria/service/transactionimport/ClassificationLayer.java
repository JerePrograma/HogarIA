package com.hogaria.service.transactionimport;

public enum ClassificationLayer {
  IGNORE_OR_NOISE,
  SOURCE_SPECIFIC,
  MERCHANT_ALIAS,
  COUNTERPARTY_ALIAS,
  STRONG_TEXT_PATTERN,
  HISTORY,
  GENERIC_FALLBACK
}
