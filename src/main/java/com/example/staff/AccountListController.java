package com.example.staff;

import com.example.bank.repository.AccountRepository.AccountOverview;
import com.example.bank.ui.Dialogs;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Function;

/** The employee's table of approved accounts. */
public class AccountListController extends StaffPage {

    @FXML private TableView<AccountOverview> tableView;
    @FXML private TableColumn<AccountOverview, String> fullname, birthgendermarital, type, accountnum, balance;

    public AccountListController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        column(fullname, r -> r.profile().fullName());
        column(birthgendermarital, r -> r.profile().birthDate() + " - " + r.profile().gender() + " - " + r.profile().maritalStatus());
        column(type, r -> r.account().type().dbValue());
        column(accountnum, r -> r.account().accountNumber());
        column(balance, r -> r.account().balance().format());
        List<AccountOverview> rows = Dialogs.attempt(() -> app().customers().approvedOverview());
        if (rows != null) tableView.setItems(FXCollections.observableArrayList(rows));
    }

    private static void column(TableColumn<AccountOverview, String> c, Function<AccountOverview, String> value) {
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
    }
}
