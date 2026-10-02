package com.example.bank.ui;

import com.example.bank.db.DataAccessException;
import com.example.bank.service.BankException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.function.Supplier;

/**
 * Messages to the user, and the one place that turns failures into messages
 * instead of hiding them. How a message is shown is up to a {@link Presenter}:
 * JavaFX alerts in the app, a recorder in automated tests.
 */
public final class Dialogs {

    /** Shows messages and asks yes/no questions. */
    public interface Presenter {
        void show(Alert.AlertType type, String title, String message);

        boolean confirm(String message);
    }

    /** The real one: JavaFX alerts. */
    public static final Presenter ALERTS = new Presenter() {
        @Override
        public void show(Alert.AlertType type, String title, String message) {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        }

        @Override
        public boolean confirm(String message) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            a.setHeaderText(null);
            return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
        }
    };

    private static Presenter presenter = ALERTS;

    private Dialogs() {
    }

    /** Replaces how messages are shown (used by the automated UI tests). */
    public static void use(Presenter p) {
        presenter = p;
    }

    public static void info(String title, String message) {
        presenter.show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void error(String message) {
        presenter.show(Alert.AlertType.ERROR, "Something went wrong", message);
    }

    public static boolean confirm(String message) {
        return presenter.confirm(message);
    }

    /**
     * Runs an action for the user. A refusal from the bank or bad input is
     * shown as it is; a database failure gets a plain explanation. Returns
     * true when the action completed.
     */
    public static boolean attempt(Runnable action) {
        return attempt(() -> {
            action.run();
            return Boolean.TRUE;
        }) != null;
    }

    public static <T> T attempt(Supplier<T> action) {
        try {
            return action.get();
        } catch (BankException | IllegalArgumentException e) {
            error(e.getMessage());
        } catch (DataAccessException e) {
            System.err.println(e.getMessage() + ": " + e.getCause());
            error(e.getMessage() + " Please try again in a moment.");
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
        return null;
    }
}
