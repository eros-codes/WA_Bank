package com.example.customer;

import com.example.bank.model.AccountType;
import com.example.bank.model.CustomerProfile;
import com.example.bank.model.Money;
import com.example.bank.service.CustomerService.Registration;
import com.example.bank.service.SignUpRules;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import com.example.bank.ui.Session;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The first screen: signing in, and signing up in four steps. The old screen
 * had a "more option" button that signed in as customer 409736 without a
 * password; it is gone.
 */
public class WelcomeController extends CustomerPage {

    private static final SecureRandom RANDOM = new SecureRandom();

    @FXML private AnchorPane start, loginpage, signpage1, signpage2, signpage3, signpage4, signdone, forgotpassnum, forgotpassmail, newpassset, mainpage;
    // signing in
    @FXML private Button enteracc, resetpin, login, signup;
    @FXML private TextField loginuser, loginpin;
    @FXML private PasswordField loginpass;
    @FXML private Label pin;
    @FXML private Hyperlink forgotpass;
    // step 1: who you are
    @FXML private TextField signname, signyear, signmonth, signday, signnation, city, street, alley, plate, floor, unit, mailingaddress, phonenum, emailsign;
    @FXML private Button signmale, signfemale, signmarried, signsingel, next1, backtologin;
    // step 2: ID and work
    @FXML private TextField idnum, issueyear, issuemonth, expireyear, expiremonth, occupation, employeradress, annual;
    @FXML private Button nationalid, next2, backtosignpage1;
    // step 3: the account
    @FXML private Button checking, saving, next3, backtosignpage2;
    @FXML private TextField deposit, dateandtime;
    // step 4: signature and terms
    @FXML private Canvas canvas;
    @FXML private Hyperlink terms;
    @FXML private CheckBox acceptcheckbox;
    @FXML private Button finished, backtosignpage3;
    // done: the new customer's details
    @FXML private Label costumername, accountno, cvv2, expired, iban, usersign, passsign;
    @FXML private Button backtologin2, backtologin3, backtologin5;

    private final Map<String, AnchorPane> panes = new LinkedHashMap<>();
    private String safetyPin;
    private String gender;
    private String marital;
    private AccountType accountType;
    private byte[] idPhoto;
    private boolean signed;
    private boolean termsOpened;

    @Override
    protected void onLoad() {
        for (AnchorPane p : List.of(start, loginpage, signpage1, signpage2, signpage3, signpage4, signdone, forgotpassnum, forgotpassmail, newpassset)) {
            panes.put(p.getId(), p);
        }
        show("start");
        enteracc.setOnAction(e -> {
            newPin();
            addTooltips();
            show("loginpage");
        });
        setUpSignIn();
        setUpSignUp();
    }

    private void show(String pane) {
        panes.forEach((id, p) -> p.setVisible(id.equals(pane)));
    }

    /* ================= signing in ================= */

    private void setUpSignIn() {
        resetpin.setOnAction(e -> newPin());
        login.setOnAction(e -> signIn());
        next(loginuser, loginpass);
        next(loginpass, loginpin);
        loginpin.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) signIn(); });
        forgotpass.setOnAction(e -> Dialogs.info("Forgot your password?",
                "For your safety, a password can only be reset at a branch. Please bring your national ID card."));
        signup.setOnAction(e -> show("signpage1"));
    }

    private void newPin() {
        safetyPin = String.valueOf(10_000 + RANDOM.nextInt(90_000));
        pin.setText(safetyPin);
        loginpin.clear();
    }

    private void signIn() {
        if (loginuser.getText().isBlank() || loginpass.getText().isEmpty()) {
            Dialogs.error("Enter your username and password.");
            return;
        }
        if (!safetyPin.equals(loginpin.getText().strip())) {
            Dialogs.error("The safety code is not right.");
            newPin();
            return;
        }
        Dialogs.attempt(() -> {
            // the password is used exactly as typed (the old screen trimmed it)
            var result = app().auth().loginCustomer(loginuser.getText().strip(), loginpass.getText());
            if (result.isEmpty()) {
                Dialogs.error("Wrong username or password.");
                newPin();
            } else if (!result.get().approved()) {
                Dialogs.info("Waiting for approval", "Your account is waiting for a bank employee to confirm it.");
                newPin();
            } else {
                Session.signIn(result.get().username(), result.get().fullName());
                open(login, CustomerScreen.DASHBOARD);
            }
        });
    }

    /* ================= signing up ================= */

    private void setUpSignUp() {
        choice(signmale, signfemale, () -> gender = "male");
        choice(signfemale, signmale, () -> gender = "female");
        choice(signmarried, signsingel, () -> marital = "married");
        choice(signsingel, signmarried, () -> marital = "single");
        choice(checking, saving, () -> accountType = AccountType.CHECKING);
        choice(saving, checking, () -> accountType = AccountType.SAVING);
        for (TextField f : new TextField[]{signyear, signmonth, signday, floor, phonenum, idnum, issueyear, issuemonth, expireyear, expiremonth, annual, deposit}) {
            Inputs.digits(f, 15);
        }
        next(signname, signyear); next(signyear, signmonth); next(signmonth, signday); next(signnation, city); next(city, street);
        next(street, alley); next(alley, plate); next(plate, floor); next(floor, unit); next(unit, mailingaddress);
        next(mailingaddress, phonenum); next(phonenum, emailsign); next(occupation, annual); next(annual, employeradress);
        emailsign.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) stepOne(); });
        employeradress.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) stepTwo(); });
        next1.setOnAction(e -> stepOne());
        nationalid.setOnAction(e -> chooseIdPhoto());
        next2.setOnAction(e -> stepTwo());
        next3.setOnAction(e -> stepThree());
        finished.setOnAction(e -> finish());
        backtologin.setOnAction(e -> show("loginpage"));
        backtosignpage1.setOnAction(e -> show("signpage1"));
        backtosignpage2.setOnAction(e -> show("signpage2"));
        backtosignpage3.setOnAction(e -> show("signpage3"));
        for (Button b : new Button[]{backtologin2, backtologin3, backtologin5}) if (b != null) b.setOnAction(e -> show("loginpage"));
        setUpSignature();
        terms.setOnAction(e -> {
            Navigator.popup(terms, "/com/example/bank/terms.fxml", "Terms and conditions", null);
            termsOpened = true;
        });
    }

    private void stepOne() {
        if (Dialogs.attempt(() -> {
            SignUpRules.fullName(signname.getText());
            SignUpRules.birthDate(signyear.getText(), signmonth.getText(), signday.getText(), LocalDate.now());
            if (marital == null) throw new IllegalArgumentException("Choose your marital status.");
            if (gender == null) throw new IllegalArgumentException("Choose your gender.");
            SignUpRules.required(signnation.getText(), "Nationality");
            SignUpRules.required(city.getText(), "City");
            SignUpRules.required(street.getText(), "Street");
            SignUpRules.required(alley.getText(), "Alley");
            SignUpRules.required(plate.getText(), "Plate");
            SignUpRules.wholeNumber(floor.getText(), "the floor");
            SignUpRules.required(unit.getText(), "Unit");
            SignUpRules.required(mailingaddress.getText(), "Postal code");
            SignUpRules.mobile(phonenum.getText());
            SignUpRules.email(emailsign.getText());
        })) show("signpage2");
    }

    private void chooseIdPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a photo of your national ID card");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(nationalid.getScene().getWindow());
        if (file == null) return;
        try {
            idPhoto = Files.readAllBytes(file.toPath());
            nationalid.getStyleClass().remove("loginbtn");
            nationalid.getStyleClass().add("loginbtnselected");
        } catch (IOException e) {
            Dialogs.error("The photo could not be read: " + e.getMessage());
        }
    }

    private void stepTwo() {
        if (Dialogs.attempt(() -> {
            SignUpRules.nationalId(idnum.getText());
            if (idPhoto == null) throw new IllegalArgumentException("Upload a photo of your national ID card.");
            SignUpRules.required(occupation.getText(), "Occupation");
            SignUpRules.required(employeradress.getText(), "Employer address");
            SignUpRules.wholeNumber(annual.getText(), "your annual income");
            if (app().customers().profileByNationalIdExists(idnum.getText())) {
                throw new IllegalArgumentException("An account with this national ID already exists.");
            }
        })) show("signpage3");
    }

    private void stepThree() {
        if (Dialogs.attempt(() -> {
            if (accountType == null) throw new IllegalArgumentException("Choose an account type.");
            SignUpRules.wholeNumber(deposit.getText(), "your first deposit");
            if (Money.parse(deposit.getText()).compareTo(com.example.bank.service.CustomerService.MINIMUM_OPENING) < 0) {
                throw new IllegalArgumentException("The first deposit must be at least 50,000 IRR.");
            }
        })) {
            dateandtime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            show("signpage4");
        }
    }

    private void setUpSignature() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.WHITE);
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

    /** The account is created here, when the customer finishes, not later when they leave the last page. */
    private void finish() {
        if (!signed) { Dialogs.error("Please sign in the box."); return; }
        if (!termsOpened) { Dialogs.error("Please read the terms and conditions."); return; }
        if (!acceptcheckbox.isSelected()) { Dialogs.error("Please accept the terms and conditions."); return; }
        String birth = String.join(".", signyear.getText(), signmonth.getText(), signday.getText());
        String address = String.join("-", city.getText(), street.getText(), alley.getText(), plate.getText(), floor.getText(), unit.getText());
        CustomerProfile details = new CustomerProfile(null, signname.getText().strip(), idnum.getText(), birth, gender, marital,
                signnation.getText().strip(), address, mailingaddress.getText().strip(), phonenum.getText(), emailsign.getText().strip(),
                occupation.getText().strip(), employeradress.getText().strip(), annual.getText(), dateandtime.getText());
        Registration done = Dialogs.attempt(() -> app().customers().signUp(details, accountType, Money.parse(deposit.getText()), idPhoto));
        if (done == null) return;
        costumername.setText(details.fullName());
        accountno.setText(done.account().formattedCardNumber());
        cvv2.setText(done.account().cvv());
        expired.setText(done.account().expiryDate());
        iban.setText(done.account().iban());
        usersign.setText(done.username());
        passsign.setText(done.password());
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        signed = false;
        show("signdone");
    }

    /* ================= small helpers ================= */

    /** Two buttons of which one is chosen: the chosen one looks selected and is disabled. */
    private static void choice(Button chosen, Button other, Runnable record) {
        chosen.setOnAction(e -> {
            record.run();
            chosen.getStyleClass().remove("loginbtn");
            if (!chosen.getStyleClass().contains("loginbtnselected")) chosen.getStyleClass().add("loginbtnselected");
            other.getStyleClass().remove("loginbtnselected");
            if (!other.getStyleClass().contains("loginbtn")) other.getStyleClass().add("loginbtn");
            chosen.setDisable(true);
            other.setDisable(false);
        });
    }

    private static void next(TextField from, TextField to) {
        from.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) to.requestFocus(); });
    }

    private void addTooltips() {
        signname.setTooltip(new Tooltip("full name"));
        signyear.setTooltip(new Tooltip("birth year"));
        signmonth.setTooltip(new Tooltip("birth month"));
        signday.setTooltip(new Tooltip("birth day"));
        for (TextField f : new TextField[]{signnation, city, street, alley, floor, unit, plate, mailingaddress, phonenum, emailsign,
                idnum, issueyear, issuemonth, expireyear, expiremonth, occupation, employeradress, annual, deposit}) {
            if (f.getPromptText() != null) f.setTooltip(new Tooltip(f.getPromptText()));
        }
    }
}
