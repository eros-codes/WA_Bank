package com.example.customer;

/**
 * A screen with several sections (transfers: card, account, IBAN; payments:
 * bills, top-ups). The section to open is handed to it when it opens,
 * instead of through a static field set by whoever opened it.
 */
public interface Sectioned {
    void showSection(String section);
}
