package com.hogaria.service.transactionimport;

public final class ImportFileTypeDetector {

    private ImportFileTypeDetector() {
    }

    public static boolean looksLikeExcelFile(byte[] bytes, String originalFilename) {
        var filename = originalFilename == null
                ? ""
                : originalFilename.toLowerCase(java.util.Locale.ROOT);

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return true;
        }

        if (bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K') {
            return true;
        }

        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0xD0
                && (bytes[1] & 0xFF) == 0xCF
                && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0
                && (bytes[4] & 0xFF) == 0xA1
                && (bytes[5] & 0xFF) == 0xB1
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0xE1;
    }
}