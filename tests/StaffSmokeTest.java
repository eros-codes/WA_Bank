package com.example.staff;

import com.example.bank.ui.Navigator;
import com.example.bank.ui.Session;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Opens every staff screen for both roles against a real database and photographs it. */
public class StaffSmokeTest extends Application {

    static final List<String> errors = new ArrayList<>();
    static final Page[] SCREENS = {Page.DASHBOARD, Page.VERIFY, Page.PAYMENTS, Page.CHANGE_PASSWORD, Page.REPORTS,
            Page.ACCOUNTS, Page.DEPOSITS, Page.TRANSACTIONS, Page.LOANS};

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> errors.add(e.toString()));
        for (StaffRole role : new StaffRole[]{EmployeeRole.INSTANCE, ManagerRole.INSTANCE}) {
            String tag = role.name().toLowerCase();
            try {
                Navigator.Loaded<Object> login = Navigator.load(role.resource(Page.LOGIN), role.controllers());
                Navigator.show(stage, login.root(), "login");
                shot(stage, tag + "-00-login");
                Session.signIn(role == ManagerRole.INSTANCE ? "1" : "emp1", "Test");
                int i = 1;
                for (Page page : SCREENS) {
                    StaffPage.open(login.root().getScene() == null ? stage.getScene().getRoot() : stage.getScene().getRoot(), role, page);
                    var root = stage.getScene().getRoot();
                    shot(stage, String.format("%s-%02d-%s", tag, i++, page.name().toLowerCase()));
                    if (root.lookup("#filter") instanceof ComboBox<?> f && f.getItems().size() > 1) {
                        for (int k = 1; k < f.getItems().size(); k++) {
                            f.getSelectionModel().select(k);
                            shot(stage, String.format("%s-%02d-%s-%d", tag, i - 1, page.name().toLowerCase(), k));
                        }
                        f.getSelectionModel().select(0);
                    }
                    if (page == Page.LOANS && root.lookup("#approved") instanceof Button approve) {
                        approve.fire();
                        System.out.println(tag + ": pressed approve on the first loan card");
                        shot(stage, String.format("%s-%02d-loans-after-approve", tag, i - 1));
                    }
                }
            } catch (Throwable e) {
                errors.add(tag + ": " + e);
                e.printStackTrace();
            }
        }
        System.out.println("ERRORS: " + errors.size());
        errors.forEach(e -> System.out.println("  " + e));
        Platform.exit();
    }

    static void shot(Stage stage, String name) throws Exception {
        var img = stage.getScene().snapshot(null);
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File("/tmp/staffshots/" + name + ".png"));
    }

    public static void main(String[] a) {
        launch(a);
    }
}
