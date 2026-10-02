package com.example.staff;

import com.example.bank.model.Employee;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class EmployeeRow {

    @FXML private Label employerusername, employerfullname;
    @FXML private TextField salary;
    @FXML private Button payment, fire;

    public void show(Employee e, Consumer<String> onPay, Runnable onFire) {
        employerusername.setText(e.username());
        employerfullname.setText(e.fullName());
        payment.setOnAction(x -> onPay.accept(salary.getText()));
        fire.setOnAction(x -> onFire.run());
    }
}
