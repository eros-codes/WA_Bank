package com.example.customer;

import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Drives the customer app through its screens and main flows, by filling fields and pressing buttons. */
public class CustomerSmokeTest extends Application {

    static final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    static final List<String> failures = Collections.synchronizedList(new ArrayList<>());
    static Stage stage;
    static int shotNo = 0;

    @Override
    public void start(Stage s) {
        stage = s;
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> { failures.add("uncaught: " + e); e.printStackTrace(); });
        Dialogs.use(new Dialogs.Presenter() {
            public void show(Alert.AlertType type, String title, String message) { messages.add(type + ": " + message); }
            public boolean confirm(String message) { messages.add("CONFIRM: " + message); return true; }
        });
        Navigator.Loaded<Object> w = Navigator.load(CustomerScreen.WELCOME.resource());
        Navigator.show(stage, w.root(), "WA Bank");
        new Thread(CustomerSmokeTest::scenario).start();
    }

    static void scenario() {
        try {
            shot("welcome");
            fx(() -> fire("enteracc"));
            fx(() -> {
                text("loginuser", "1001");
                text("loginpass", "pass1234");
                text("loginpin", ((Label) find("pin")).getText());
                fire("login");
            });
            pause(300);
            check(find("menu") != null, "signed in and on the dashboard");
            shot("dashboard");
            Object[][] screens = {{CustomerScreen.TRANSFERS, "cardtocard"}, {CustomerScreen.TRANSFERS, "accounttransfer"},
                    {CustomerScreen.TRANSFERS, "ibantransfer"}, {CustomerScreen.PAYMENTS, "billpage"}, {CustomerScreen.PAYMENTS, "charge"},
                    {CustomerScreen.CHEQUE_BOOK, null}, {CustomerScreen.MY_CHEQUES, null}, {CustomerScreen.LOAN, null},
                    {CustomerScreen.OPEN_DEPOSIT, null}, {CustomerScreen.MY_DEPOSITS, null}, {CustomerScreen.HISTORY, null},
                    {CustomerScreen.ACCOUNTS, null}, {CustomerScreen.NEW_ACCOUNT, null}, {CustomerScreen.PROFILE, null}};
            for (Object[] sc : screens) {
                go((CustomerScreen) sc[0], (String) sc[1]);
                shot(sc[0] + (sc[1] == null ? "" : "-" + sc[1]));
            }

            System.out.println("-- card to card through the screen");
            go(CustomerScreen.TRANSFERS, "cardtocard");
            fx(() -> {
                text("sourcecard", "6037990000000001"); text("cardyear", "2030"); text("cardmounth", "05"); text("cardcvv", "123");
                text("destinationcard", "6037990000000002"); text("cardamount", "100000");
                fire("comfirmcard");
            });
            check(visible("cardotp"), "the confirmation step shows the recipient: " + label("cardownername") + ", " + label("carddestinationbank"));
            String code = otp(() -> { fire("cardcomfirm"); fire("cardsendotp"); });
            fx(() -> { text("cardgetotp", code); fire("cardtransfer"); });
            check("SUCCESSFUL".equals(label("cardstatusrec")), "receipt says " + label("cardstatusrec") + ", trace " + label("cardtractionrec"));
            shot("card-receipt");

            System.out.println("-- negative amount cannot even be typed");
            go(CustomerScreen.TRANSFERS, "cardtocard");
            fx(() -> text("cardamount", "-5000"));
            check(((TextField) find("cardamount")).getText().isEmpty(), "the amount field refused \"-5000\"");

            System.out.println("-- water bill");
            go(CustomerScreen.PAYMENTS, "billpage");
            fx(() -> { fire("wbill"); fire("paythebill"); });
            check(label("amountbill").startsWith("3,270,000"), "bill shown with 9% tax: " + label("amountbill"));
            fx(() -> { text("billcard", "6037990000000001"); text("billyear", "2030"); text("billmonth", "05"); text("billcvv", "123"); });
            String billCode = otp(() -> fire("billsendotp"));
            fx(() -> { text("billgetotp", billCode); fire("paymentdone"); });
            check(find("payed").isVisible() && !label("brecpid").equals("-"), "bill paid, payment id " + label("brecpid"));
            shot("bill-receipt");

            System.out.println("-- a short-term deposit");
            go(CustomerScreen.OPEN_DEPOSIT, null);
            fx(() -> { fire("shortd"); fire("onemonthshort"); fire("shortonemonthinterest"); text("shortamount", "1000000");
                ((CheckBox) find("shortaccept")).setSelected(true); fire("shortdone"); });
            check(visible("payment"), "payment step: " + label("depositamount"));
            fx(() -> text("card", "1000000001"));
            String depCode = otp(() -> fire("sendotp"));
            fx(() -> { text("getotp", depCode); fire("donepayment"); });
            pause(300);
            check(find("seedeposits") != null && ((Parent) find("seedeposits")).getChildrenUnmodifiable().size() >= 2, "now on My deposits, with the new one listed");
            shot("my-deposits");

            System.out.println("-- a cheque");
            go(CustomerScreen.CHEQUE_BOOK, null);
            fx(() -> {
                text("amount", "5000"); text("recipentname", "Ali Rezaei"); text("duedate", java.time.LocalDate.now().plusDays(20).format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")));
                Canvas c = (Canvas) find("sign");
                c.getOnMousePressed().handle(new MouseEvent(MouseEvent.MOUSE_PRESSED, 10, 10, 10, 10, MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false, null));
                fire("done");
            });
            pause(300);
            check(messages.stream().anyMatch(m -> m.contains("has been issued")), "cheque issued");
            shot("my-cheques");

            System.out.println("-- change password");
            go(CustomerScreen.PROFILE, null);
            fx(() -> { text("oldpass", "pass1234"); fire("change"); text("mewpass", "NewPass123"); text("newcom", "NewPass123"); fire("submit"); });
            check(messages.stream().anyMatch(m -> m.contains("password has been changed")), "password changed");
            check(!((TextField) find("user")).isEditable(), "the username field cannot be edited");

            go(CustomerScreen.HISTORY, null);
            pause(300);
            AtomicReference<Integer> rows = new AtomicReference<>();
            fx(() -> rows.set(((Parent) find("seetrn")).getChildrenUnmodifiable().size()));
            check(rows.get() >= 2, "the history lists the transfer made above, with the earlier one (" + rows.get() + " rows)");
            shot("history");
            go(CustomerScreen.ACCOUNTS, null);
            shot("accounts-after");
        } catch (Throwable e) {
            failures.add("scenario: " + e);
            e.printStackTrace();
        }
        System.out.println("MESSAGES SHOWN TO THE USER:");
        messages.forEach(m -> System.out.println("   " + m.replace("\n", " ")));
        System.out.println("FAILURES: " + failures.size());
        failures.forEach(f -> System.out.println("   " + f));
        Platform.runLater(Platform::exit);
    }

    /* ---- helpers ---- */

    static String otp(Runnable request) throws Exception {
        int before = messages.size();
        fx(request);
        for (int i = 0; i < 70; i++) {
            pause(100);
            for (int k = before; k < messages.size(); k++) {
                Matcher m = Pattern.compile("Your code is: (\\d{5})").matcher(messages.get(k));
                if (m.find()) return m.group(1);
            }
        }
        throw new IllegalStateException("no one-time code arrived; messages: " + messages.subList(before, messages.size()));
    }

    static void go(CustomerScreen screen, String section) throws Exception {
        fx(() -> {
            Node from = stage.getScene().getRoot();
            if (section == null) CustomerPage.open(from, screen);
            else CustomerPage.openSection(from, screen, section);
        });
        pause(250);
    }

    static Node find(String id) {
        return stage.getScene().getRoot().lookup("#" + id);
    }

    static void fire(String id) {
        Node n = find(id);
        if (n == null) throw new IllegalStateException("no #" + id);
        ((ButtonBase) n).fire();
    }

    static void text(String id, String value) {
        ((TextInputControl) find(id)).setText(value);
    }

    static String label(String id) {
        AtomicReference<String> r = new AtomicReference<>();
        try { fx(() -> r.set(((Labeled) find(id)).getText())); } catch (Exception e) { throw new RuntimeException(e); }
        return r.get() == null ? "" : r.get();
    }

    static boolean visible(String id) {
        AtomicReference<Boolean> r = new AtomicReference<>();
        try { fx(() -> r.set(find(id).isVisible())); } catch (Exception e) { throw new RuntimeException(e); }
        return r.get();
    }

    static void check(boolean ok, String what) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + what);
        if (!ok) failures.add(what);
    }

    static void shot(String name) throws Exception {
        String file = String.format("/tmp/custshots/%02d-%s.png", shotNo++, name.toLowerCase());
        fx(() -> {
            try { ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png", new File(file)); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    static void fx(Runnable r) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try { r.run(); } catch (Throwable t) { err.set(t); } finally { done.countDown(); }
        });
        done.await();
        if (err.get() != null) throw new RuntimeException(err.get());
    }

    static void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    public static void main(String[] a) {
        launch(a);
    }
}
