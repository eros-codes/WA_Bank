package com.example.staff;

import com.example.bank.app.AppContext;
import com.example.bank.ui.Navigator;
import com.example.bank.ui.Session;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;

import java.util.Map;

/**
 * The base of every staff screen. It knows the signed-in role, wires the
 * side menu once for all screens (every screen used to repeat it), and
 * leaves each screen only its own work in {@link #onLoad()}.
 */
public abstract class StaffPage {

    /** The side menu's buttons by fx:id. Two spellings of the bills button exist in the FXML. */
    private static final Map<String, Page> MENU = Map.of(
            "verifyaccount", Page.VERIFY,
            "billpayments", Page.PAYMENTS,
            "billpayment", Page.PAYMENTS,
            "changepassword", Page.CHANGE_PASSWORD,
            "financialreports", Page.REPORTS,
            "listofaccounts", Page.ACCOUNTS,
            "deposit", Page.DEPOSITS,
            "transactions", Page.TRANSACTIONS,
            "loanrequests", Page.LOANS);

    protected final StaffRole role;

    protected StaffPage(StaffRole role) {
        this.role = role;
    }

    protected static AppContext app() {
        return AppContext.get();
    }

    /** Called by the FXML loader once the fields are filled in. */
    @FXML
    public final void initialize() {
        onLoad();
    }

    /** The screen's own setup. */
    protected void onLoad() {
    }

    /** Whether the screen's "back" button goes to the dashboard (a screen can use it for itself). */
    protected boolean backGoesToDashboard() {
        return true;
    }

    /** Connects whichever menu buttons this screen has. */
    void wireMenu(Parent root) {
        MENU.forEach((id, page) -> {
            if (root.lookup("#" + id) instanceof ButtonBase b) b.setOnAction(e -> open(b, page));
        });
        if (root.lookup("#logout") instanceof ButtonBase b) b.setOnAction(e -> {
            Session.signOut();
            open(b, Page.LOGIN);
        });
        if (backGoesToDashboard() && root.lookup("#back") instanceof ButtonBase b) b.setOnAction(e -> open(b, Page.DASHBOARD));
    }

    protected final void open(Node from, Page page) {
        open(from, role, page);
    }

    static void open(Node from, StaffRole role, Page page) {
        Navigator.<Object>go(from, role.resource(page), role.name() + " · " + page.title(), role.controllers(), loaded -> {
            if (loaded.controller() instanceof StaffPage p) p.wireMenu(loaded.root());
        });
    }
}
