package net.tessel.fx.gallery.pages;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import net.tessel.fx.components.TesselFileChooser;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.components.ContentDialog;
import net.tessel.fx.components.TesselDialog;
import net.tessel.fx.components.TextBlock;
import net.tessel.fx.gallery.Page;

public class DialogsPage extends Page {

    private final Label result = new Label("Result: -");

    public DialogsPage() {
        super("Dialogs", "In-window content dialogs, window dialogs and standard alerts.");

        Button confirm = new Button("Confirm delete");
        confirm.setOnAction(e -> {
            ContentDialog d = new ContentDialog("Delete file?", "report.pdf will be deleted. This can't be undone.");
            d.setPrimaryButtonText("Delete");
            d.setCloseButtonText("Cancel");
            d.setPrimaryDestructive(true);
            report(d.showAndWait(confirm));
        });

        Button save = new Button("Three buttons");
        save.setOnAction(e -> {
            ContentDialog d = new ContentDialog("Save changes?", "Your changes will be lost if you don't save them.");
            d.setPrimaryButtonText("Save");
            d.setSecondaryButtonText("Don't save");
            d.setCloseButtonText("Cancel");
            report(d.showAndWait(save));
        });

        Button rename = new Button("With a form");
        rename.setOnAction(e -> {
            TextField name = new TextField("report.pdf");
            name.setPromptText("File name");
            ContentDialog d = new ContentDialog("Rename file", new VBox(8, new TextBlock("Enter a new name:"), name));
            d.setPrimaryButtonText("Rename");
            d.setCloseButtonText("Cancel");
            d.primaryButtonDisabledProperty().bind(name.textProperty().isEmpty());
            d.setOnClosing(r -> r != ContentDialog.Result.PRIMARY || !name.getText().contains("/"));
            d.show(rename).thenAccept(r -> javafx.application.Platform.runLater(() -> report(r)));
            javafx.application.Platform.runLater(name::requestFocus);
        });

        add(section("Content dialog",
                row(confirm, save, rename),
                result,
                caption("Drawn over the window with a dimmed backdrop. Esc cancels, Enter presses the default button, Tab stays inside.")));

        Button window = Tessel.style(new Button("Open a Tessel dialog"), ButtonStyle.ACCENT);
        window.setOnAction(e -> {
            Window owner = window.getScene().getWindow();
            TesselDialog d = new TesselDialog(owner, "Rename file");
            Button ok = Tessel.style(new Button("Rename"), ButtonStyle.ACCENT);
            Button cancel = new Button("Cancel");
            ok.setOnAction(x -> d.close());
            cancel.setOnAction(x -> d.close());
            TextField field = new TextField("report.pdf");
            field.setPrefWidth(320);
            VBox box = new VBox(12, new TextBlock("New name"), field, new javafx.scene.layout.HBox(8, ok, cancel));
            box.setPadding(new javafx.geometry.Insets(8, 24, 24, 24));
            d.setContent(box);
            d.showDialog();
        });
        Button alert = new Button("Standard Alert");
        alert.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to continue?");
            a.setHeaderText("Continue?");
            a.initOwner(alert.getScene().getWindow());
            a.showAndWait();
        });
        addFileChooser();
        add(section("Window dialogs", row(window, alert),
                caption("TesselDialog is a real window with the Tessel chrome. A plain javafx.scene.control.Alert is themed by the style sheet.")));
    }

    private void addFileChooser() {
        Label chosen = new Label("Chosen: -");
        chosen.getStyleClass().add("tsl-text-caption");
        TesselFileChooser chooser = new TesselFileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.gif"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.md"));

        Button open = new Button("Open file...");
        open.setOnAction(e -> {
            chooser.setTitle("Open file");
            java.io.File f = chooser.showOpenDialog(open.getScene().getWindow());
            chosen.setText("Chosen: " + f);
        });
        Button many = new Button("Open several files...");
        many.setOnAction(e -> {
            chooser.setTitle("Open files");
            chosen.setText("Chosen: " + chooser.showOpenMultipleDialog(many.getScene().getWindow()));
        });
        Button save = new Button("Save as...");
        save.setOnAction(e -> {
            chooser.setTitle("Save as");
            chooser.setInitialFileName("untitled");
            chosen.setText("Chosen: " + chooser.showSaveDialog(save.getScene().getWindow()));
        });
        Button folder = new Button("Choose folder...");
        folder.setOnAction(e -> {
            chooser.setTitle("Choose a folder");
            chosen.setText("Chosen: " + chooser.showDirectoryDialog(folder.getScene().getWindow()));
        });
        add(section("File chooser", row(open, many, save, folder), chosen,
                caption("TesselFileChooser has the API of javafx.stage.FileChooser, but looks like Tessel instead of the operating system.")));
    }

    private void report(ContentDialog.Result r) {
        result.setText("Result: " + r);
    }
}
