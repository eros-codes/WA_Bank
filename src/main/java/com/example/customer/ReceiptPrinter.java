package com.example.customer;

import com.example.bank.ui.Dialogs;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Saves a receipt, as it looks on screen, to a PDF the customer chooses. */
final class ReceiptPrinter {

    private ReceiptPrinter() {
    }

    static void saveAsPdf(Node receipt) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save the receipt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = chooser.showSaveDialog(receipt.getScene().getWindow());
        if (file == null) return;
        BufferedImage image = SwingFXUtils.fromFXImage(receipt.snapshot(new SnapshotParameters(), null), null);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            float scale = Math.min(page.getMediaBox().getWidth() / image.getWidth(), page.getMediaBox().getHeight() / image.getHeight());
            float w = image.getWidth() * scale, h = image.getHeight() * scale;
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(pdfImage, (page.getMediaBox().getWidth() - w) / 2, (page.getMediaBox().getHeight() - h) / 2, w, h);
            }
            document.save(file);
            Dialogs.info("Saved", "The receipt was saved to " + file.getName() + ".");
        } catch (IOException e) {
            Dialogs.error("The receipt could not be saved: " + e.getMessage());
        }
    }
}
