package com.example.staff;

import com.example.bank.model.Cheque;
import com.example.bank.ui.Dialogs;
import com.example.bank.app.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** A cheque waiting for review, with accept and deny. */
public class ChequeReviewCard {

    @FXML private Label checkingnumber, checkingamount, checkingsender, checkingrecipenumber, checkingduedate, checkingnowdate;
    @FXML private Button acceptcheck, deniedcheck;

    public void show(Cheque c, Runnable afterReview) {
        checkingnumber.setText(String.valueOf(c.number()));
        checkingamount.setText(c.amount().format());
        checkingsender.setText(c.issuerName());
        checkingrecipenumber.setText(c.recipientName());
        checkingduedate.setText(c.dueDate() == null ? "" : c.dueDate().format(TransferCard.WHEN));
        checkingnowdate.setText(c.date() == null ? "" : c.date().format(TransferCard.WHEN));
        acceptcheck.setOnAction(e -> review(c, true, afterReview));
        deniedcheck.setOnAction(e -> review(c, false, afterReview));
    }

    private static void review(Cheque c, boolean accept, Runnable afterReview) {
        if (Dialogs.attempt(() -> AppContext.get().cheques().review(c.number(), accept))) afterReview.run();
    }
}
