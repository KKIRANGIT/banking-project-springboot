package com.example.bankingpaymentservice.util;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "N/A";
        }

        String normalized = accountNumber.trim();
        if (normalized.length() <= 4) {
            return normalized;
        }

        return "*".repeat(normalized.length() - 4) + normalized.substring(normalized.length() - 4);
    }
}
