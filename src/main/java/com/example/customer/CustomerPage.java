package com.example.customer;

import com.example.bank.app.AppContext;
import com.example.bank.ui.Navigator;
import com.example.bank.ui.Session;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;

import java.util.function.Consumer;

/** The base of every customer screen: its own setup in {@link #onLoad()}, and moving to other screens. */
public abstract class CustomerPage {

    protected static AppContext app() {
        return AppContext.get();
    }

    protected static String me() {
        return Session.username();
    }

    @FXML
    public final void initialize() {
        onLoad();
    }

    protected abstract void onLoad();

    /** Opens another screen in this window. */
    protected static void open(Node from, CustomerScreen screen) {
        open(from, screen, null);
    }

    /** Opens another screen and hands its controller to {@code setup} before it is shown. */
    @SuppressWarnings("unchecked")
    protected static <C> void open(Node from, CustomerScreen screen, Consumer<C> setup) {
        Navigator.<Object>go(from, screen.resource(), "WA Bank · " + screen.title(), null, loaded -> {
            if (setup != null) setup.accept((C) loaded.controller());
        });
    }

    /** Opens a screen at one of its sections. */
    protected static void openSection(Node from, CustomerScreen screen, String section) {
        CustomerPage.<Sectioned>open(from, screen, page -> page.showSection(section));
    }

    /** Makes each of these buttons return to the home screen. */
    protected static void homeButtons(ButtonBase... buttons) {
        for (ButtonBase b : buttons) if (b != null) b.setOnAction(e -> open(b, CustomerScreen.DASHBOARD));
    }
}
