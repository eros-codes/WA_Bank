package com.example.customer;

import com.example.bank.model.LoanApplication;
import com.example.bank.model.Money;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Applying for a loan: amount, term, four documents, signature. */
public class LoanApplicationController extends CustomerPage {

    private static final Money MAXIMUM = Money.of(5_000_000_000L);

    @FXML private TextField loadamount, intrestrate, date;
    @FXML private Button oneyear, twoyear, threeyear, fouryear, fiveyear;
    @FXML private Button idupload, eploymentupload, guarantorupload, deedupload, done, back;
    @FXML private Canvas sign;
    @FXML private CheckBox accept;
    @FXML private Hyperlink terms;

    private SignaturePad signature;
    private String term;
    private String rate;
    private byte[] idDocument, employmentDocument, collateralDocument, deedDocument;

    @Override
    protected void onLoad() {
        homeButtons(back);
        Inputs.digits(loadamount, 13);
        loadamount.textProperty().addListener((o, a, now) -> loadamount.setStyle(!now.isEmpty() && Money.parse(now).compareTo(MAXIMUM) > 0
                ? "-fx-border-color: red; -fx-border-width: 2px; -fx-border-radius: 15px;"
                : "-fx-border-color: #a3b4c4; -fx-border-width: 2px; -fx-border-radius: 15px;"));
        Button[] terms = {oneyear, twoyear, threeyear, fouryear, fiveyear};
        String[] rates = {"12%", "16%", "18%", "20%", "22%"};
        for (int i = 0; i < terms.length; i++) {
            int years = i + 1;
            String r = rates[i];
            Button b = terms[i];
            b.setOnAction(e -> {
                // the old screen stored the interest number ("12") as the term; the term is the number of years
                term = years == 1 ? "1 year" : years + " years";
                rate = r;
                intrestrate.setText(r);
                for (Button other : terms) other.setDisable(other == b);
            });
        }
        idupload.setOnAction(e -> idDocument = choose("Upload ID or Birth Certificate", idupload));
        eploymentupload.setOnAction(e -> employmentDocument = choose("Upload Employment Documents", eploymentupload));
        guarantorupload.setOnAction(e -> collateralDocument = choose("Upload Collateral and Guarantor", guarantorupload));
        deedupload.setOnAction(e -> deedDocument = choose("Upload Property Deed or Lease Agreement", deedupload));
        accept.setOnAction(e -> {
            if (accept.isSelected()) date.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        });
        this.terms.setOnAction(e -> Navigator.popup(this.terms, "/com/example/bank/terms.fxml", "Terms and conditions", null));
        signature = new SignaturePad(sign);
        done.setOnAction(e -> apply());
    }

    private byte[] choose(String title, Button button) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File file = chooser.showOpenDialog(button.getScene().getWindow());
        if (file == null) return null;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            button.getStyleClass().add("loginbtnselected");
            return bytes;
        } catch (IOException ex) {
            Dialogs.error("The file could not be read: " + ex.getMessage());
            return null;
        }
    }

    private void apply() {
        if (loadamount.getText().isBlank()) { Dialogs.error("Enter the amount you want to borrow."); return; }
        Money amount = Money.parse(loadamount.getText());
        if (amount.compareTo(MAXIMUM) > 0) { Dialogs.error("The most you can borrow is " + MAXIMUM.format() + " IRR."); return; }
        if (term == null) { Dialogs.error("Choose the term of the loan."); return; }
        if (!accept.isSelected()) { Dialogs.error("Please accept the terms and conditions."); return; }
        if (!signature.isSigned()) { Dialogs.error("Please sign the application."); return; }
        LoanApplication application = new LoanApplication(me(), amount, term, rate, date.getText(),
                idDocument, employmentDocument, collateralDocument, deedDocument);
        if (Dialogs.attempt(() -> { app().loans().apply(application); })) {
            Dialogs.info("Submitted", "Your loan application has been sent. A bank employee will review it.");
            open(done, CustomerScreen.DASHBOARD);
        }
    }
}
