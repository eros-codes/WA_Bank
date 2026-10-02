package com.example.staff;

import com.example.bank.model.Employee;
import com.example.bank.model.Money;
import com.example.bank.repository.AccountRepository.AccountOverview;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.TilePane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The manager's list of customers and of employees. */
public class ManageAccountsController extends StaffPage {

    private static final String CUSTOMERS = "User";
    private static final String EMPLOYEES = "Employer";

    @FXML private ComboBox<String> filter;
    @FXML private TilePane tileee;

    public ManageAccountsController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        filter.getItems().setAll(CUSTOMERS, EMPLOYEES);
        filter.setValue(CUSTOMERS);
        filter.valueProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        Dialogs.attempt(() -> {
            if (CUSTOMERS.equals(filter.getValue())) {
                // one card per customer, not per account
                Map<String, AccountOverview> customers = new LinkedHashMap<>();
                for (AccountOverview a : app().customers().approvedOverview()) customers.putIfAbsent(a.profile().username(), a);
                Navigator.<AccountOverview, CustomerRow>fill(tileee, List.copyOf(customers.values()), role.resource(Page.CUSTOMER_ROW),
                        (row, a) -> row.show(a, () -> manage(a.profile().username())));
            } else {
                Navigator.<Employee, EmployeeRow>fill(tileee, app().staff().all(), role.resource(Page.EMPLOYEE_ROW),
                        (row, e) -> row.show(e, salary -> paySalary(e, salary), () -> fire(e)));
            }
            Tiles.margin(tileee, 10);
        });
    }

    private void manage(String username) {
        Navigator.Loaded<ManageCustomerController> window = Navigator.popup(tileee, role.resource(Page.MANAGE_CUSTOMER), Page.MANAGE_CUSTOMER.title(), role.controllers());
        window.controller().open(username, this::refresh);
    }

    private void paySalary(Employee e, String salary) {
        if (Dialogs.attempt(() -> app().staff().paySalary(e.username(), Money.parse(salary)))) {
            Dialogs.info("Paid", "The salary has been paid to " + e.fullName() + ".");
        }
    }

    private void fire(Employee e) {
        if (!Dialogs.confirm("Remove " + e.fullName() + " from the staff?")) return;
        if (Dialogs.attempt(() -> app().staff().remove(e.username()))) refresh();
    }
}
