package net.tessel.fx.gallery.pages;

import java.io.File;
import java.util.List;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import net.tessel.fx.components.FileBrowser;
import net.tessel.fx.gallery.Page;

public class FilesPage extends Page {

    public FilesPage() {
        super("Files", "The file browser behind the Tessel file chooser, embedded in a page.");

        FileBrowser browser = new FileBrowser(FileBrowser.Mode.OPEN_MULTIPLE);
        browser.getFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        browser.getFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        browser.getFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.md", "*.csv"));
        browser.setPrefHeight(470);

        Label result = new Label("Double-click a file, or press Enter in the name field.");
        result.getStyleClass().add("tsl-text-caption");
        browser.setOnAccept(() -> {
            List<File> files = browser.resolveResult();
            if (!files.isEmpty()) {
                result.setText("Chosen: " + files);
            }
        });
        add(section("FileBrowser", browser, result,
                caption("Click the empty part of the address bar to type a path. Ctrl+click or Shift+click select several files; "
                        + "Alt+Left / Alt+Right / Alt+Up navigate, Ctrl+Shift+N creates a folder.")));
    }
}
