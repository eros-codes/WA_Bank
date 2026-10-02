package com.example.bank.model;

import java.time.LocalDateTime;

/** One transfer, with the fee the sender paid for it. */
public record LedgerEntry(
        int id,
        String kind,
        String sourceName,
        String sourceCard,
        String sourceAccount,
        String sourceIban,
        Money amount,
        String destinationName,
        String destinationCard,
        String destinationAccount,
        String destinationIban,
        String destinationBank,
        LocalDateTime time,
        Money fee) {
}
