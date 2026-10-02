package com.example.staff;

import com.example.bank.repository.AccountRepository.AccountOverview;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class CustomerRow {

    @FXML private Label accountusername, accountfullname, accountnationalid;
    @FXML private Button manage;

    public void show(AccountOverview a, Runnable onManage) {
        accountusername.setText(a.profile().username());
        accountfullname.setText(a.profile().fullName());
        accountnationalid.setText(a.profile().nationalId());
        manage.setOnAction(e -> onManage.run());
    }
}
