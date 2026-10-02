package com.example.staff;

import com.example.bank.repository.AccountRepository.PendingAccount;
import com.example.bank.ui.Dialogs;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;

/** Reviewing new accounts, one at a time, oldest first. Approving or denying acts on that one account only. */
public class VerifyAccountController extends StaffPage {

    @FXML private TextField fullname, phonenumber, type, birth, id, work, annual, gender, job, marital;
    @FXML private ImageView idpic;
    @FXML private Label checking;
    @FXML private Button verify, deny, address;

    private PendingAccount current;

    public VerifyAccountController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        verify.setOnAction(e -> decide(true));
        deny.setOnAction(e -> decide(false));
        address.setOnAction(e -> showAddressCheck());
        showNext();
    }

    private void showNext() {
        clear();
        current = Dialogs.attempt(() -> app().customers().nextPending().orElse(null));
        if (current == null) return;
        var p = current.profile();
        fullname.setText(p.fullName());
        id.setText(p.nationalId());
        birth.setText(p.birthDate());
        gender.setText(p.gender());
        marital.setText(p.maritalStatus());
        phonenumber.setText(p.phoneNumber());
        job.setText(p.occupation());
        annual.setText(p.annualIncome());
        type.setText(current.account().type().dbValue());
        work.setText(p.address());
        byte[] photo = current.idPhoto();
        idpic.setImage(photo == null || photo.length == 0 ? null : new Image(new ByteArrayInputStream(photo)));
    }

    private void decide(boolean approve) {
        if (current == null) {
            Dialogs.info("Nothing to review", "There are no accounts waiting to be verified.");
            return;
        }
        String number = current.account().accountNumber();
        if (!approve && !Dialogs.confirm("Deny and delete this application?")) return;
        if (Dialogs.attempt(() -> {
            if (approve) app().customers().approve(number);
            else app().customers().deny(number);
        })) {
            Dialogs.info("Done", approve ? "The account has been verified." : "The application has been denied.");
            showNext();
        }
    }

    /** The address check of the original design: a short animation that ends in "matching". */
    private void showAddressCheck() {
        Timeline t = new Timeline();
        t.setCycleCount(7);
        int[] n = {0};
        t.getKeyFrames().add(new KeyFrame(Duration.seconds(0.5), e -> {
            n[0]++;
            checking.setText(".".repeat(n[0] % 3 + 1));
            if (n[0] == 6) {
                checking.setText("matching");
                checking.setStyle("-fx-text-fill: green;");
            }
        }));
        t.play();
    }

    private void clear() {
        for (TextField f : new TextField[]{fullname, id, birth, gender, marital, phonenumber, job, annual, type, work}) f.clear();
        idpic.setImage(null);
        checking.setText(null);
        checking.setStyle("");
    }
}
