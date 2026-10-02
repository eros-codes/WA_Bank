package com.example.staff;

import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.TilePane;

/** Every fixed-term deposit. */
public class DepositsController extends StaffPage {

    @FXML private TilePane tile;

    public DepositsController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        Dialogs.attempt(() -> {
            Navigator.fillCards(tile, app().deposits().all(), role.resource(Page.DEPOSIT_CARD));
            Tiles.margin(tile, 10);
        });
    }
}
