package com.example.staff;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

/** The staff home screen: just the menu, which the base class wires. */
public class DashboardController extends StaffPage {

    @FXML private AnchorPane start;

    public DashboardController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        start.setVisible(true);
    }
}
