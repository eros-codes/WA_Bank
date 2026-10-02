package com.example.customer;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * A field that suggests the customer's own number as they type: the typed
 * part and the rest of the suggestion are shown side by side, and Enter
 * accepts the suggestion. The three transfer forms used three copies of this.
 */
final class SuggestionField {

    private SuggestionField() {
    }

    /**
     * @param onAccept called when the suggestion is accepted with Enter
     * @param onChange called when the text no longer matches an accepted suggestion
     */
    static void attach(TextField field, TextFlow view, String suggestion, Runnable onAccept, Runnable onChange) {
        // the screens style their suggestions in two ways; the text flow's own class says which
        boolean classic = view.getStyleClass().contains("styled-textflow");
        Text typed = new Text();
        typed.getStyleClass().add(classic ? "typed-text" : "text2-content");
        Text rest = new Text(suggestion == null ? "" : suggestion);
        rest.getStyleClass().add(classic ? "remaining-text" : "remain2-content");
        view.getChildren().setAll(typed, rest);
        Platform.runLater(field::requestFocus);
        if (suggestion == null) return;
        field.textProperty().addListener((o, before, now) -> {
            typed.setText(now);
            rest.setText(now.isEmpty() || suggestion.startsWith(now) ? suggestion.substring(Math.min(now.length(), suggestion.length())) : "");
            if (now.isEmpty() || !now.equals(suggestion)) onChange.run();
            field.positionCaret(now.length());
        });
        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                field.setText(suggestion);
                rest.setText("");
                onAccept.run();
                field.positionCaret(suggestion.length());
            }
        });
    }
}
