package com.example.customer;

import com.example.bank.model.Cheque;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

/** The cheques the customer has written. */
public class MyChequesController extends CustomerPage {

    @FXML private VBox seechecks;
    @FXML private Button addcheckbook, back;

    @Override
    protected void onLoad() {
        homeButtons(back);
        addcheckbook.setOnAction(e -> open(addcheckbook, CustomerScreen.CHEQUE_BOOK));
        List<Cheque> mine = Dialogs.attempt(() -> app().cheques().writtenBy(me()));
        if (mine == null) return;
        Navigator.fillCards(seechecks, mine, CustomerScreen.CHEQUE_ROW.resource());
        seechecks.getChildren().forEach(n -> VBox.setMargin(n, new Insets(10)));
    }
}
