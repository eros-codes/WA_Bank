package com.example.customer;

import com.example.bank.ui.Session;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

/** The customer's home: the menu, and shortcuts to the common screens. */
public class DashboardController extends CustomerPage {

    @FXML private AnchorPane mainpage, menubar;
    @FXML private Button menu, closemenu, logout, cardtran, hesabtran, ibantran, bills, charge, addcheck, check, loan,
            opendeposit, seedeposit, transactions, myaccounts, editprof;
    @FXML private ImageView imbiils, imcard, imcharge, imcheck, imdeposit, iminternal;

    @Override
    protected void onLoad() {
        menubar.setVisible(false);
        menu.setOnAction(e -> menubar.setVisible(true));
        closemenu.setOnAction(e -> menubar.setVisible(false));
        logout.setOnAction(e -> {
            Session.signOut();
            open(logout, CustomerScreen.WELCOME);
        });
        section(cardtran, imcard, CustomerScreen.TRANSFERS, "cardtocard");
        section(hesabtran, iminternal, CustomerScreen.TRANSFERS, "accounttransfer");
        section(ibantran, null, CustomerScreen.TRANSFERS, "ibantransfer");
        section(bills, imbiils, CustomerScreen.PAYMENTS, "billpage");
        section(charge, imcharge, CustomerScreen.PAYMENTS, "charge");
        screen(addcheck, null, CustomerScreen.CHEQUE_BOOK);
        screen(check, imcheck, CustomerScreen.MY_CHEQUES);
        screen(loan, null, CustomerScreen.LOAN);
        screen(opendeposit, null, CustomerScreen.OPEN_DEPOSIT);
        screen(seedeposit, imdeposit, CustomerScreen.MY_DEPOSITS);
        screen(transactions, null, CustomerScreen.HISTORY);
        screen(myaccounts, null, CustomerScreen.ACCOUNTS);
        screen(editprof, null, CustomerScreen.PROFILE);
    }

    private static void screen(Button button, Node shortcut, CustomerScreen target) {
        button.setOnAction(e -> open(button, target));
        if (shortcut != null) shortcut.setOnMouseClicked(e -> open(shortcut, target));
    }

    private static void section(Button button, Node shortcut, CustomerScreen target, String section) {
        button.setOnAction(e -> openSection(button, target, section));
        if (shortcut != null) shortcut.setOnMouseClicked(e -> openSection(shortcut, target, section));
    }
}
