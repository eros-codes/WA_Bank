package com.example.customer;

import com.example.bank.model.HistoryItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** One line of the history: money in is green, money out is red. */
public class HistoryRow {

    @FXML private Label trntype, trnamount;
    @FXML private Button details;

    public void show(HistoryItem item, Runnable onDetails) {
        trntype.setText(item.title());
        trnamount.setText(item.amount().format());
        if (item.kind() == HistoryItem.Kind.TRANSFER) {
            trnamount.setStyle(item.amount().isNegative() ? "-fx-text-fill: #cc2d2d;" : "-fx-text-fill: #2e8b57;");
        }
        details.setOnAction(e -> onDetails.run());
    }
}
