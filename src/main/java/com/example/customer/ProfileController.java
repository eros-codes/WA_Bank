package com.example.customer;

import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * Changing the password. The old screen checked the old password for the
 * username typed in an editable field, then saved the new password for
 * whatever username was in that field at the end, so anyone could change
 * anyone's password. The username now comes from the session and cannot be
 * edited, and the change is one step that checks the old password.
 */
public class ProfileController extends CustomerPage {

    @FXML private TextField user;
    @FXML private PasswordField oldpass, mewpass, newcom;
    @FXML private Button change, submit, back;

    @Override
    protected void onLoad() {
        homeButtons(back);
        user.setText(me());
        user.setEditable(false);
        mewpass.setVisible(false);
        newcom.setVisible(false);
        submit.setVisible(false);
        change.setOnAction(e -> {
            if (oldpass.getText().isEmpty()) {
                Dialogs.error("Enter your current password.");
                return;
            }
            mewpass.setVisible(true);
            newcom.setVisible(true);
            submit.setVisible(true);
            mewpass.requestFocus();
        });
        mewpass.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) newcom.requestFocus(); });
        submit.setOnAction(e -> save());
    }

    private void save() {
        if (!mewpass.getText().equals(newcom.getText())) {
            Dialogs.error("The new password and its confirmation do not match.");
            return;
        }
        if (Dialogs.attempt(() -> app().auth().changeCustomerPassword(me(), oldpass.getText(), mewpass.getText()))) {
            Dialogs.info("Done", "Your password has been changed.");
            oldpass.clear();
            mewpass.clear();
            newcom.clear();
            mewpass.setVisible(false);
            newcom.setVisible(false);
            submit.setVisible(false);
        }
    }
}
