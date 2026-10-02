package com.example.staff;

import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/** Changing the signed-in staff member's password. */
public class ChangePasswordController extends StaffPage {

    @FXML private TextField oldpass, newpass, compass;
    @FXML private Button save;

    public ChangePasswordController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        save.setOnAction(e -> change());
    }

    private void change() {
        if (!newpass.getText().equals(compass.getText())) {
            Dialogs.error("The new password and its confirmation do not match.");
            return;
        }
        if (Dialogs.attempt(() -> role.changePassword(app().auth(), Session.username(), oldpass.getText(), newpass.getText()))) {
            Dialogs.info("Done", "Your password has been changed.");
            oldpass.clear();
            newpass.clear();
            compass.clear();
        }
    }
}
