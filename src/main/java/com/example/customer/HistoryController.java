package com.example.customer;

import com.example.bank.model.HistoryItem;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The customer's history. Transfers now include money received: the old
 * screen searched by the sender's name, so incoming transfers never showed.
 */
public class HistoryController extends CustomerPage {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private ComboBox<String> trncombo;
    @FXML private VBox seetrn;
    @FXML private AnchorPane recipe, rec;
    @FXML private Label l1, l2, l3, l4, l5, l6, l1show, l2show, l3show, l4show, l5show, l6show;
    @FXML private Button back, pdf, backtohome;

    @Override
    protected void onLoad() {
        recipe.setVisible(false);
        homeButtons(back);
        backtohome.setOnAction(e -> recipe.setVisible(false));
        pdf.setOnAction(e -> ReceiptPrinter.saveAsPdf(rec));
        trncombo.getItems().setAll("Transactions", "Bills", "Charge");
        trncombo.setOnAction(e -> load());
        trncombo.setValue("Transactions");
        load();
    }

    private void load() {
        HistoryItem.Kind kind = switch (trncombo.getValue()) {
            case "Bills" -> HistoryItem.Kind.BILL;
            case "Charge" -> HistoryItem.Kind.TOP_UP;
            default -> HistoryItem.Kind.TRANSFER;
        };
        List<HistoryItem> items = Dialogs.attempt(() -> app().reports().historyOf(me()).stream().filter(i -> i.kind() == kind).toList());
        if (items == null) return;
        Navigator.<HistoryItem, HistoryRow>fill(seetrn, items, CustomerScreen.HISTORY_ROW.resource(), (row, item) -> row.show(item, () -> details(item)));
        seetrn.getChildren().forEach(n -> VBox.setMargin(n, new Insets(5, 10, 5, 10)));
    }

    private void details(HistoryItem item) {
        Dialogs.attempt(() -> {
            switch (item.kind()) {
                case TRANSFER -> app().reports().transfer(item.id()).ifPresent(t -> fill(
                        "Transaction Number", String.valueOf(t.id()), "Type", t.kind(), "Source Card", t.sourceCard(),
                        "Destination Card", t.destinationCard(), "Source Account", t.sourceAccount(), "Destination Account", t.destinationAccount()));
                case BILL -> app().reports().bill(item.id()).ifPresent(b -> fill(
                        "Transaction Number", String.valueOf(b.transactionNo()), "Bill Type", b.billType(), "Bill ID", String.valueOf(b.billId()),
                        "Bill Amount", b.amount().format(), "Account Number", b.accountNo(), "Payment Date", b.paidAt() == null ? "" : b.paidAt().format(WHEN)));
                case TOP_UP -> app().reports().topUp(item.id()).ifPresent(t -> fill(
                        "Transaction Number", String.valueOf(t.transactionNo()), "Telephone Number", t.phoneNumber(), "Account Number", t.accountNo(),
                        "Amount", t.amount().format(), "Time", t.time() == null ? "" : t.time().format(WHEN), "", ""));
            }
            recipe.setVisible(true);
        });
    }

    private void fill(String... pairs) {
        Label[] names = {l1, l2, l3, l4, l5, l6};
        Label[] values = {l1show, l2show, l3show, l4show, l5show, l6show};
        for (int i = 0; i < 6; i++) {
            names[i].setText(pairs[2 * i]);
            values[i].setText(pairs[2 * i + 1] == null ? "-" : pairs[2 * i + 1]);
        }
    }
}
