package com.example.staff;

import javafx.application.Application;

/** The manager app. */
public class ManagerApp extends StaffApp {

    @Override
    protected StaffRole role() {
        return ManagerRole.INSTANCE;
    }

    public static void main(String[] args) {
        Application.launch(ManagerApp.class, args);
    }
}
