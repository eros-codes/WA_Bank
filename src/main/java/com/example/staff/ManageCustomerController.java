package com.example.staff;

import com.example.bank.app.AppContext;
import com.example.bank.model.CustomerProfile;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * The manager's view of one customer. The old screen showed the customer's
 * password; its save failed every time (it wrote to columns that do not
 * exist); and removing a customer deleted their money with them.
 */
public class ManageCustomerController {

    @FXML private TextField usernameacc, passwordacc, fullnameacc, idacc, genderacc, incomeacc, addressacc, phoneacc,
            emalacc, typeacc, maritalacc, postalacc, workacc, workaddressacc;
    @FXML private Button saveacc, toban, employerpromote, back;

    private CustomerProfile profile;
    private Runnable onChange = () -> { };

    public void open(String username, Runnable afterChange) {
        onChange = afterChange;
        profile = Dialogs.attempt(() -> AppContext.get().customers().profile(username).orElse(null));
        if (profile == null) {
            close();
            return;
        }
        usernameacc.setText(profile.username());
        passwordacc.setText("••••••••");
        fullnameacc.setText(profile.fullName());
        idacc.setText(profile.nationalId());
        genderacc.setText(profile.gender());
        incomeacc.setText(profile.annualIncome());
        addressacc.setText(profile.address());
        phoneacc.setText(profile.phoneNumber());
        emalacc.setText(profile.email());
        maritalacc.setText(profile.maritalStatus());
        postalacc.setText(profile.postalCode());
        workacc.setText(profile.occupation());
        workaddressacc.setText(profile.employerAddress());
        typeacc.setText(AppContext.get().customers().accountsOf(username).stream().map(a -> a.type().dbValue()).distinct()
                .reduce((x, y) -> x + ", " + y).orElse(""));
        for (TextField readOnly : new TextField[]{usernameacc, passwordacc, idacc, incomeacc, typeacc}) readOnly.setEditable(false);
        saveacc.setOnAction(e -> save());
        toban.setOnAction(e -> remove());
        employerpromote.setOnAction(e -> promote());
        back.setOnAction(e -> close());
    }

    private void save() {
        CustomerProfile changed = new CustomerProfile(profile.username(), fullnameacc.getText().strip(), profile.nationalId(),
                profile.birthDate(), genderacc.getText().strip(), maritalacc.getText().strip(), profile.nationality(),
                addressacc.getText().strip(), postalacc.getText().strip(), phoneacc.getText().strip(), emalacc.getText().strip(),
                workacc.getText().strip(), workaddressacc.getText().strip(), profile.annualIncome(), profile.creationDate());
        if (Dialogs.attempt(() -> AppContext.get().customers().updateProfile(changed))) {
            profile = changed;
            Dialogs.info("Saved", "The customer's details have been updated.");
            onChange.run();
        }
    }

    private void remove() {
        if (!Dialogs.confirm("Remove " + profile.fullName() + " and all of their accounts?")) return;
        if (Dialogs.attempt(() -> AppContext.get().customers().removeCustomer(profile.username()))) {
            onChange.run();
            close();
        }
    }

    private void promote() {
        if (Dialogs.attempt(() -> AppContext.get().staff().promote(profile.username()))) {
            Dialogs.info("Done", profile.fullName() + " is now an employee and can sign in with the same username and password.");
        }
    }

    private void close() {
        ((Stage) back.getScene().getWindow()).close();
    }
}
