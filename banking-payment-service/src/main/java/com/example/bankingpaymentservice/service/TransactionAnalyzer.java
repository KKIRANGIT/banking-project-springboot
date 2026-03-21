package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TransactionAnalyzer {

    private TransactionAnalyzer() {
    }

    public static List<Transaction> findTopFiveByAmount(List<Transaction> list) {
        return list.stream()
                .sorted((left, right) -> right.getAmount().compareTo(left.getAmount()))
                .limit(5)
                .toList();
    }

    public static Map<TransactionStatus, List<Transaction>> groupByStatus(List<Transaction> list) {
        return list.stream()
                .collect(Collectors.groupingBy(Transaction::getStatus));
    }

    public static Map<TransactionType, BigDecimal> getTotalAmountByType(List<Transaction> list) {
        return list.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }

    public static Set<String> findDuplicateAccountNumbers(List<Transaction> list) {
        return list.stream()
                .collect(Collectors.groupingBy(Transaction::getAccountNumber, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
