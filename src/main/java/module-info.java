module com.example.bank {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.sql;
    requires org.apache.pdfbox;

    // the customer app
    opens com.example.customer to javafx.fxml, javafx.graphics;
    // the employee and manager apps
    opens com.example.staff to javafx.fxml, javafx.graphics;
}
