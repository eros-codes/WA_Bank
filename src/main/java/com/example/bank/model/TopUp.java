package com.example.bank.model;

import java.time.LocalDateTime;

/** Mobile credit bought for a phone number. */
public record TopUp(int transactionNo, String username, String payerName, String accountNo, String phoneNumber, Money amount, LocalDateTime time) {
}
