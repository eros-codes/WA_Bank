package com.example.bank.model;

/** One line of a customer's history: a transfer, a bill or a top-up. */
public record HistoryItem(Kind kind, int id, String title, Money amount) {
    public enum Kind { TRANSFER, BILL, TOP_UP }
}
