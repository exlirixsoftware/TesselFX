package net.tessel.fx.components;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;

/**
 * A file chooser dialog in the Tessel style, with the API of {@link javafx.stage.FileChooser} (which opens a native
 * operating system dialog that no theme can change).
 *
 * <pre>{@code
 * TesselFileChooser chooser = new TesselFileChooser();
 * chooser.setTitle("Open picture");
 * chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
 * File file = chooser.showOpenDialog(window);         // null when cancelled
 * List<File> files = chooser.showOpenMultipleDialog(window);
 * File target = chooser.showSaveDialog(window);       // asks before replacing an existing file
 * File folder = chooser.showDirectoryDialog(window);
 * }</pre>
 *
 * It shows a breadcrumb address bar, a places sidebar, back / forward / up / new folder, and list, details and tile
 * views; see {@link FileBrowser}. The dialog opens in the folder the last dialog of this chooser was closed in.
 */
public class TesselFileChooser {

    private final StringProperty title = new SimpleStringProperty("");
    private final ObjectProperty<File> initialDirectory = new SimpleObjectProperty<>();
    private final StringProperty initialFileName = new SimpleStringProperty("");
    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();
    private final ObjectProperty<ExtensionFilter> selectedExtensionFilter = new SimpleObjectProperty<>();

    public TesselFileChooser() {
    }

    /** @return the chosen file, or null when the dialog was cancelled */
    public File showOpenDialog(Window owner) {
        List<File> files = show(owner, FileBrowser.Mode.OPEN, "Open");
        return files.isEmpty() ? null : files.get(0);
    }

    /** @return the chosen files, or null when the dialog was cancelled */
    public List<File> showOpenMultipleDialog(Window owner) {
        List<File> files = show(owner, FileBrowser.Mode.OPEN_MULTIPLE, "Open");
        return files.isEmpty() ? null : files;
    }

    /** @return the file to write, or null when the dialog was cancelled */
    public File showSaveDialog(Window owner) {
        List<File> files = show(owner, FileBrowser.Mode.SAVE, "Save");
        return files.isEmpty() ? null : files.get(0);
    }

    /** @return the chosen folder, or null when the dialog was cancelled */
    public File showDirectoryDialog(Window owner) {
        List<File> files = show(owner, FileBrowser.Mode.DIRECTORY, "Select folder");
        return files.isEmpty() ? null : files.get(0);
    }

    private List<File> show(Window owner, FileBrowser.Mode mode, String primaryText) {
        FileBrowser browser = new FileBrowser(mode);
        browser.getFilters().setAll(extensionFilters);
        ExtensionFilter selected = selectedExtensionFilter.get();
        if (selected != null && extensionFilters.contains(selected)) {
            browser.setSelectedFilter(selected);
        }
        File start = initialDirectory.get();
        if (start != null && start.isDirectory()) {
            browser.setDirectory(start);
        }
        if (mode == FileBrowser.Mode.SAVE) {
            browser.setFileName(initialFileName.get());
        }

        String text = title.get().isEmpty()
                ? (mode == FileBrowser.Mode.SAVE ? "Save as" : mode == FileBrowser.Mode.DIRECTORY ? "Select a folder" : "Open")
                : title.get();
        TesselDialog dialog = new TesselDialog(owner, text);

        AtomicReference<List<File>> result = new AtomicReference<>(List.of());
        Button primary = Tessel.style(new Button(primaryText), ButtonStyle.ACCENT);
        Button cancel = new Button("Cancel");
        HBox buttons = new HBox(8, primary, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getStyleClass().add("tsl-fc-buttons");

        Runnable accept = () -> {
            List<File> chosen = browser.resolveResult();
            if (chosen.isEmpty()) {
                return;
            }
            File file = chosen.get(0);
            if (mode == FileBrowser.Mode.SAVE && file.exists()) {
                ContentDialog confirm = new ContentDialog("Replace file?",
                        "\"" + file.getName() + "\" already exists in this folder. Do you want to replace it?");
                confirm.setPrimaryButtonText("Replace");
                confirm.setCloseButtonText("Cancel");
                confirm.setPrimaryDestructive(true);
                confirm.show(dialog).thenAccept(r -> {
                    if (r == ContentDialog.Result.PRIMARY) {
                        result.set(chosen);
                        dialog.close();
                    }
                });
                return;
            }
            result.set(chosen);
            dialog.close();
        };
        browser.setOnAccept(accept);
        primary.setOnAction(e -> accept.run());
        cancel.setOnAction(e -> dialog.close());

        BorderPane content = new BorderPane();
        content.setCenter(browser);
        content.setBottom(buttons);
        dialog.setContent(content);
        dialog.showDialog();

        if (browser.getDirectory() != null) {
            initialDirectory.set(browser.getDirectory());
        }
        selectedExtensionFilter.set(browser.getSelectedFilter());
        return result.get();
    }

    public final String getTitle() {
        return title.get();
    }

    public final void setTitle(String title) {
        this.title.set(title == null ? "" : title);
    }

    public final StringProperty titleProperty() {
        return title;
    }

    /** The folder to start in; null (the default) starts in the user's home folder. */
    public final File getInitialDirectory() {
        return initialDirectory.get();
    }

    public final void setInitialDirectory(File dir) {
        initialDirectory.set(dir);
    }

    public final ObjectProperty<File> initialDirectoryProperty() {
        return initialDirectory;
    }

    /** The suggested name in a save dialog. */
    public final String getInitialFileName() {
        return initialFileName.get();
    }

    public final void setInitialFileName(String name) {
        initialFileName.set(name == null ? "" : name);
    }

    public final StringProperty initialFileNameProperty() {
        return initialFileName;
    }

    /** The file types offered in the combo box; none shows all files. */
    public final ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    /** The filter that is selected first, and the one that was selected when the last dialog was closed. */
    public final ExtensionFilter getSelectedExtensionFilter() {
        return selectedExtensionFilter.get();
    }

    public final void setSelectedExtensionFilter(ExtensionFilter filter) {
        selectedExtensionFilter.set(filter);
    }

    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        return selectedExtensionFilter;
    }
}
