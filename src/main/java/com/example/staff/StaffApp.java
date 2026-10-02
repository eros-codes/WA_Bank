package com.example.staff;

import com.example.bank.ui.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

/** Starts a staff app at its sign-in screen. The two apps differ only in their role. */
public abstract class StaffApp extends Application {

    protected abstract StaffRole role();

    @Override
    public void start(Stage stage) {
        StaffRole role = role();
        Navigator.Loaded<Object> login = Navigator.load(role.resource(Page.LOGIN), role.controllers());
        Navigator.show(stage, login.root(), role.name() + " · " + Page.LOGIN.title());
    }
}
