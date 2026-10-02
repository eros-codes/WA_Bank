package com.example.bank.model;

/** The personal details a customer gives when signing up. */
public record CustomerProfile(
        String username,
        String fullName,
        String nationalId,
        String birthDate,
        String gender,
        String maritalStatus,
        String nationality,
        String address,
        String postalCode,
        String phoneNumber,
        String email,
        String occupation,
        String employerAddress,
        String annualIncome,
        String creationDate) {
}
