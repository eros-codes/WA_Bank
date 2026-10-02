package com.example.staff;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;

/** Sizing shared by the screens that show cards in a tile pane. */
final class Tiles {

    private Tiles() {
    }

    /** Gives every card the same size and keeps long text inside it. */
    static void fix(Pane tile, double width, double height, Insets margin) {
        for (Node n : tile.getChildren()) {
            if (n instanceof Region r) {
                r.setPrefWidth(width);
                r.setPrefHeight(height);
                r.setMinHeight(height);
                r.setMaxHeight(height);
            }
            if (n instanceof Pane p) {
                for (Node child : p.getChildren()) {
                    if (child instanceof Label l) {
                        l.setWrapText(true);
                        l.setMaxWidth(width - 30);
                    }
                }
            }
            TilePane.setMargin(n, margin);
        }
    }

    static void margin(Pane tile, double gap) {
        for (Node n : tile.getChildren()) TilePane.setMargin(n, new Insets(gap));
    }
}
