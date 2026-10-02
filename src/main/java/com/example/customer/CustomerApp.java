package com.example.customer;

import com.example.bank.ui.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

/** The customer app. */
public class CustomerApp extends Application {

    @Override
    public void start(Stage stage) {
        Navigator.Loaded<Object> welcome = Navigator.load(CustomerScreen.WELCOME.resource());
        Navigator.show(stage, welcome.root(), "WA Bank");
    }

    public static void main(String[] args) {
        Application.launch(CustomerApp.class, args);
    }
}
