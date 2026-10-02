package com.example.bank.repository;

import com.example.bank.model.BillPayment;
import com.example.bank.model.Money;
import com.example.bank.model.TopUp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Bills paid and phone credit bought (the bills and charge tables). */
public interface PaymentRepository {

    boolean billAlreadyPaid(int billId);

    int recordBill(Connection c, BillPayment bill, Money tax) throws SQLException;

    int recordTopUp(Connection c, TopUp topUp, Money tax) throws SQLException;

    List<BillPayment> findBills(String username);

    List<TopUp> findTopUps(String username);

    Optional<BillPayment> findBill(int transactionNo);

    Optional<TopUp> findTopUp(int transactionNo);

    List<BillPayment> findAllBills();

    List<TopUp> findAllTopUps();

    int countBills();
}
