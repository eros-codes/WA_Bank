package com.example.staff;

import javafx.application.Application;

/** The employee app. */
public class EmployeeApp extends StaffApp {

    @Override
    protected StaffRole role() {
        return EmployeeRole.INSTANCE;
    }

    public static void main(String[] args) {
        Application.launch(EmployeeApp.class, args);
    }
}
