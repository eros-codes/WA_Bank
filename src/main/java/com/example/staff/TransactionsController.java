package com.example.staff;

import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;

/** Every transfer, the cheques waiting for review, and the cheques already reviewed. */
public class TransactionsController extends StaffPage {

    private static final String TRANSFERS = "Transactions";
    private static final String PENDING = "Pending Checks";

    @FXML private ComboBox<String> filter;
    @FXML private TilePane tile;
    @FXML private ScrollPane scroll;

    public TransactionsController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        tile.setHgap(10);
        tile.setVgap(10);
        tile.setPadding(new Insets(10));
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        filter.getItems().setAll(TRANSFERS, PENDING, role.reviewedChequesLabel());
        filter.setValue(TRANSFERS);
        filter.valueProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        Dialogs.attempt(() -> {
            Insets margin = new Insets(20, 15, 20, 15);
            if (TRANSFERS.equals(filter.getValue())) {
                Navigator.fillCards(tile, app().reports().allTransfers(), role.resource(Page.TRANSFER_CARD));
                Tiles.fix(tile, 250, 469, margin);
            } else if (PENDING.equals(filter.getValue())) {
                Navigator.<com.example.bank.model.Cheque, ChequeReviewCard>fill(tile, app().cheques().pending(), role.resource(Page.CHEQUE_REVIEW_CARD),
                        (card, cheque) -> card.show(cheque, this::refresh));
                Tiles.fix(tile, 250, 450, margin);
            } else {
                Navigator.fillCards(tile, app().cheques().reviewed(), role.resource(Page.REVIEWED_CHEQUE_CARD));
                Tiles.fix(tile, 250, 404, margin);
            }
        });
    }
}
