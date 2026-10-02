package com.example.customer;

import com.example.bank.model.TermDeposit;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

/** The customer's deposits. */
public class MyDepositsController extends CustomerPage {

    @FXML private VBox seedeposits;
    @FXML private Button adddeposit, back;

    @Override
    protected void onLoad() {
        homeButtons(back);
        adddeposit.setOnAction(e -> open(adddeposit, CustomerScreen.OPEN_DEPOSIT));
        List<TermDeposit> mine = Dialogs.attempt(() -> app().deposits().of(me()));
        if (mine == null) return;
        Navigator.<TermDeposit, DepositCard>fill(seedeposits, mine, CustomerScreen.DEPOSIT_CARD.resource(), DepositCard::show);
        seedeposits.getChildren().forEach(n -> VBox.setMargin(n, new Insets(10)));
    }
}
