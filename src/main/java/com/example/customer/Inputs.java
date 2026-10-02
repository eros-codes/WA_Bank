package com.example.customer;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Input restrictions shared by the forms. */
final class Inputs {

    private Inputs() {
    }

    /** Only digits, at most {@code max} of them. */
    static void digits(TextField field, int max) {
        field.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("\\d{0," + max + "}") ? change : null));
    }

    /** Only digits, and the typed text mirrored into a label (the card picture shows what is typed). */
    static void digits(TextField field, int max, Label mirror) {
        digits(field, max);
        if (mirror != null) field.textProperty().addListener((o, a, b) -> mirror.setText(b));
    }
}
