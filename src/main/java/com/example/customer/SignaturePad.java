package com.example.customer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** A box to sign in with the mouse. Four screens had their own copy of this. */
final class SignaturePad {

    private final Canvas canvas;
    private boolean signed;

    SignaturePad(Canvas canvas) {
        this.canvas = canvas;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        canvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
            signed = true;
        });
        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });
    }

    boolean isSigned() {
        return signed;
    }

    void clear() {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        signed = false;
    }
}
