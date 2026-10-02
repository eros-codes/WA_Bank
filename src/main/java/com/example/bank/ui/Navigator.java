package com.example.bank.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Loads screens and moves between them inside one window. The old code opened
 * a new window for every click and only hid the previous one, so hidden
 * windows piled up for as long as the app ran.
 */
public final class Navigator {

    private Navigator() {
    }

    /** What a loaded FXML gives back: its root node and its controller. */
    public record Loaded<C>(Parent root, C controller) {
    }

    public static <C> Loaded<C> load(String resource, Callback<Class<?>, Object> controllers) {
        URL url = Navigator.class.getResource(resource);
        if (url == null) throw new IllegalStateException("Screen not found: " + resource);
        FXMLLoader loader = new FXMLLoader(url);
        if (controllers != null) loader.setControllerFactory(controllers);
        try {
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open " + resource, e);
        }
    }

    public static <C> Loaded<C> load(String resource) {
        return load(resource, null);
    }

    /** Replaces the content of the window that {@code from} is in. */
    public static <C> Loaded<C> go(Node from, String resource, String title, Callback<Class<?>, Object> controllers, Consumer<Loaded<C>> afterLoad) {
        Loaded<C> loaded = load(resource, controllers);
        if (afterLoad != null) afterLoad.accept(loaded);
        show((Stage) from.getScene().getWindow(), loaded.root(), title);
        return loaded;
    }

    public static void show(Stage stage, Parent root, String title) {
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }

    /** A small window on top of the current one (details, forms); closes with its own button or the X. */
    public static <C> Loaded<C> popup(Node owner, String resource, String title, Callback<Class<?>, Object> controllers) {
        Loaded<C> loaded = load(resource, controllers);
        Stage stage = new Stage();
        stage.initOwner(owner.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(loaded.root()));
        stage.setTitle(title);
        stage.setResizable(false);
        stage.show();
        return loaded;
    }

    /** Fills a pane with one card per item. */
    public static <T, C> void fill(Pane pane, List<T> items, String cardResource, BiConsumer<C, T> bind) {
        pane.getChildren().clear();
        for (T item : items) {
            Loaded<C> card = load(cardResource);
            bind.accept(card.controller(), item);
            pane.getChildren().add(card.root());
        }
    }

    /** Fills a pane with cards that show themselves. */
    public static <T> void fillCards(Pane pane, List<T> items, String cardResource) {
        Navigator.<T, Card<T>>fill(pane, items, cardResource, Card::show);
    }
}
