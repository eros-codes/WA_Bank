package com.example.customer;

import com.example.bank.model.Money;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Writing a cheque to another customer of the bank. It is reviewed by an employee before it counts. */
public class WriteChequeController extends CustomerPage {

    private static final List<DateTimeFormatter> DATES = List.of(DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    @FXML private TextField number, amount, recipentname, duedate, date;
    @FXML private Label issuename;
    @FXML private Canvas sign;
    @FXML private Button done, backtochecks;

    private SignaturePad signature;

    @Override
    protected void onLoad() {
        backtochecks.setOnAction(e -> open(backtochecks, CustomerScreen.MY_CHEQUES));
        date.setText(LocalDate.now().format(DATES.get(0)));
        date.setEditable(false);
        number.setText("given when issued");
        number.setEditable(false);
        issuename.setText(app().customers().primaryAccount(me()).map(a -> a.fullName()).orElse(""));
        Inputs.digits(amount, 15);
        amount.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) recipentname.requestFocus(); });
        recipentname.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) duedate.requestFocus(); });
        duedate.setPromptText("yyyy.MM.dd");
        signature = new SignaturePad(sign);
        done.setOnAction(e -> issue());
    }

    private void issue() {
        if (amount.getText().isBlank()) { Dialogs.error("Enter the amount."); return; }
        if (recipentname.getText().isBlank()) { Dialogs.error("Enter the recipient's full name."); return; }
        if (!signature.isSigned()) { Dialogs.error("The cheque must be signed."); return; }
        LocalDateTime due = parse(duedate.getText());
        if (due == null) { Dialogs.error("Enter the due date like " + LocalDate.now().plusMonths(1).format(DATES.get(0)) + "."); return; }
        Integer issued = Dialogs.attempt(() -> app().cheques().write(me(), recipentname.getText(), Money.parse(amount.getText()), due));
        if (issued == null) return;
        Dialogs.info("Cheque issued", "Cheque number " + issued + " has been issued and is waiting for review.");
        open(done, CustomerScreen.MY_CHEQUES);
    }

    private static LocalDateTime parse(String text) {
        for (DateTimeFormatter f : DATES) {
            try {
                return LocalDate.parse(text.strip(), f).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }
}
