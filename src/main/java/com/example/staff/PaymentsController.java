package com.example.staff;

import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.TilePane;

/** Paid bills and mobile top-ups. */
public class PaymentsController extends StaffPage {

    private static final String BILLS = "Bills";
    private static final String TOP_UPS = "Charge";

    @FXML private ComboBox<String> filter;
    @FXML private TilePane tile;

    public PaymentsController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        tile.setPadding(new Insets(10));
        filter.getItems().setAll(BILLS, TOP_UPS);
        filter.setValue(BILLS);
        filter.valueProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        Dialogs.attempt(() -> {
            if (BILLS.equals(filter.getValue())) Navigator.fillCards(tile, app().reports().allBills(), role.resource(Page.BILL_CARD));
            else Navigator.fillCards(tile, app().reports().allTopUps(), role.resource(Page.TOP_UP_CARD));
            Tiles.margin(tile, 10);
        });
    }
}
