package com.example.customer;

import com.example.bank.ui.Dialogs;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.security.SecureRandom;
import java.util.function.BooleanSupplier;

/**
 * A one-time code for confirming a transfer. There is no SMS gateway in this
 * project, so the code is shown in a message a few seconds after it is
 * requested, standing in for the text message. It expires with the countdown.
 */
final class OtpChallenge {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SECONDS = 120;

    private final Label timer;
    private final Button send;
    private String code;
    private Timeline countdown;

    OtpChallenge(Button send, Label timer) {
        this(send, timer, () -> true);
    }

    /** @param ready checked when the code is requested; it shows its own message when the answer is no */
    OtpChallenge(Button send, Label timer, BooleanSupplier ready) {
        this.send = send;
        this.timer = timer;
        send.setOnAction(e -> {
            if (ready.getAsBoolean()) issue();
        });
    }

    private void issue() {
        code = String.valueOf(10_000 + RANDOM.nextInt(90_000));
        send.setDisable(true);
        int[] left = {SECONDS};
        if (countdown != null) countdown.stop();
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            left[0]--;
            timer.setText(String.format("%d:%02d", left[0] / 60, left[0] % 60));
            if (left[0] <= 0) {
                code = null; // expired
                send.setDisable(false);
            }
        }));
        countdown.setCycleCount(SECONDS);
        countdown.play();
        String shown = code;
        PauseTransition sms = new PauseTransition(Duration.seconds(4));
        sms.setOnFinished(e -> Dialogs.info("Your one-time code", "Your code is: " + shown));
        sms.play();
    }

    /** True once, for the right code before it expires; the code cannot be used again. */
    boolean accept(String entered) {
        if (code == null || entered == null || !code.equals(entered.strip())) return false;
        code = null;
        return true;
    }

    void reset() {
        code = null;
        if (countdown != null) countdown.stop();
        timer.setText("");
        send.setDisable(false);
    }
}
