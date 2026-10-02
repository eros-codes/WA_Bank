package com.example.staff;

import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.security.SecureRandom;

/**
 * Staff sign-in, with the on-screen safety code. The old screen also had a
 * hidden button that signed in as user 409736 without a password; it is gone.
 */
public class StaffLoginController extends StaffPage {

    private static final SecureRandom RANDOM = new SecureRandom();

    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private TextField safety;
    @FXML private Label random;
    @FXML private Button refresh;
    @FXML private Button enter;

    private String code;

    public StaffLoginController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        newCode();
        refresh.setOnAction(e -> newCode());
        enter.setOnAction(e -> signIn());
        username.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) password.requestFocus(); });
        password.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) safety.requestFocus(); });
        safety.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) signIn(); });
    }

    private void newCode() {
        code = String.format("%05d", RANDOM.nextInt(100_000));
        random.setText(code);
        safety.clear();
    }

    private void signIn() {
        if (!code.equals(safety.getText().strip())) {
            Dialogs.error("The safety code does not match. Please try again.");
            newCode();
            return;
        }
        if (username.getText().isBlank() || password.getText().isEmpty()) {
            Dialogs.error("Enter your username and password.");
            return;
        }
        Dialogs.attempt(() -> {
            var who = role.signIn(app().auth(), username.getText().strip(), password.getText());
            if (who.isEmpty()) {
                Dialogs.error("Wrong username or password.");
                newCode();
                return;
            }
            Session.signIn(who.get().username(), who.get().fullName());
            open(enter, Page.DASHBOARD);
        });
    }
}
