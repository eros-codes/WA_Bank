package com.example.bank.model;

import java.time.LocalDateTime;

/** A cheque written by a customer and waiting for, or past, an employee's review. */
public record Cheque(int number, String issuerName, Money amount, String recipientName, LocalDateTime date, LocalDateTime dueDate, ChequeStatus status) {
}
