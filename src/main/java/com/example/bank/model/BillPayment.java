package com.example.bank.model;

import java.time.LocalDateTime;

/** A utility bill paid from an account. */
public record BillPayment(int transactionNo, String username, String billType, int billId, Money amount,
                          String paidBy, String accountNo, LocalDateTime paidAt, String dueDate) {
}
