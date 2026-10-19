package net.tessel.fx.components;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.util.StringConverter;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Severity;
import net.tessel.fx.Tessel;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * A file browser pane: breadcrumb address bar (click the empty part to type a path), places sidebar, back / forward /
 * up / new folder, list, details and tile views, extension filters, and a name field. {@link TesselFileChooser} puts
 * it in a dialog; it can also be embedded in a window.
 *
 * <pre>{@code
 * FileBrowser browser = new FileBrowser(FileBrowser.Mode.OPEN_MULTIPLE);
 * browser.setDirectory(new File(System.getProperty("user.home")));
 * browser.getFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
 * browser.setOnAccept(() -> open(browser.resolveResult()));   // double click or Enter
 * }</pre>
 *
 * Keyboard: Backspace or Alt+Up goes up, Alt+Left / Alt+Right go back and forward, F5 refreshes, Ctrl+L edits the
 * address, Ctrl+Shift+N creates a folder, Enter opens the selected folder.
 */
public class FileBrowser extends BorderPane {

    /** What the browser is used for. */
    public enum Mode {
        /** Pick one existing file. */
        OPEN,
        /** Pick any number of existing files. */
        OPEN_MULTIPLE,
        /** Type or pick the name of a file to write. */
        SAVE,
        /** Pick a folder; only folders are listed. */
        DIRECTORY
    }

    /** How the files of a folder are shown. */
    public enum ViewMode { LIST, DETAILS, TILES }

    /** One row of the listing. */
    static final class Entry {
        final File file;
        final String name;
        final boolean directory;
        final boolean hidden;
        final long size;
        final long modified;

        Entry(File file) {
            this.file = file;
            this.name = file.getName().isEmpty() ? file.getPath() : file.getName();
            this.directory = file.isDirectory();
            this.hidden = file.isHidden();
            this.size = directory ? -1 : file.length();
            this.modified = file.lastModified();
        }

        String type() {
            if (directory) {
                return "File folder";
            }
            int dot = name.lastIndexOf('.');
            return dot > 0 && dot < name.length() - 1 ? name.substring(dot + 1).toUpperCase(Locale.ROOT) + " file" : "File";
        }
    }

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass EDITING = PseudoClass.getPseudoClass("editing");
    private static final Comparator<Entry> DIRECTORIES_FIRST =
            Comparator.comparing((Entry e) -> !e.directory);
    private static final Comparator<Entry> BY_NAME = Comparator.comparing(e -> e.name, String.CASE_INSENSITIVE_ORDER);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.OPEN);
    private final ObjectProperty<ViewMode> viewMode = new SimpleObjectProperty<>(ViewMode.DETAILS);
    private final ObjectProperty<File> directory = new SimpleObjectProperty<>();
    private final BooleanProperty showHidden = new SimpleBooleanProperty(false);
    private final StringProperty fileName = new SimpleStringProperty("");
    private final ObservableList<ExtensionFilter> filters = FXCollections.observableArrayList();
    private final ObjectProperty<ExtensionFilter> selectedFilter = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onAccept = new SimpleObjectProperty<>();

    private final ObservableList<Entry> entries = FXCollections.observableArrayList();
    private final ObservableList<Entry> selection = FXCollections.observableArrayList();
    private final Deque<File> backStack = new ArrayDeque<>();
    private final Deque<File> forwardStack = new ArrayDeque<>();

    private final Button backButton = Tessel.style(new Button(), ButtonStyle.ICON);
    private final Button forwardButton = Tessel.style(new Button(), ButtonStyle.ICON);
    private final Button upButton = Tessel.style(new Button(), ButtonStyle.ICON);
    private final BreadcrumbBar crumbs = new BreadcrumbBar();
    private final TextField pathField = new TextField();
    private final StackPane addressHost = new StackPane();
    private final VBox places = new VBox(2);
    private final TableView<Entry> table = new TableView<>(entries);
    private final ListView<Entry> list = new ListView<>(entries);
    private final Tiles tiles = new Tiles();
    private final StackPane views = new StackPane();
    private final Label nameLabel = new Label();
    private final TextField nameField = new TextField();
    private final ComboBox<ExtensionFilter> filterBox = new ComboBox<>(filters);
    private final Label placeholder = new Label();

    private final List<Runnable> placeUpdaters = new ArrayList<>();
    private boolean syncing;
    private long loadId;
    private File pendingSelect;

    public FileBrowser() {
        this(Mode.OPEN);
    }

    public FileBrowser(Mode mode) {
        getStyleClass().add("tsl-file-browser");
        setPrefSize(900, 560);
        this.mode.set(mode);

        setTop(buildToolbar());
        setLeft(buildPlaces());
        setCenter(buildContent());
        setBottom(buildFooter());

        this.mode.addListener((o, a, b) -> modeChanged());
        viewMode.addListener((o, a, b) -> applyViewMode());
        showHidden.addListener((o, a, b) -> reload());
        selectedFilter.addListener((o, a, b) -> reload());
        directory.addListener((o, a, b) -> directoryChanged());
        filters.addListener((ListChangeListener<ExtensionFilter>) c -> {
            if (selectedFilter.get() == null && !filters.isEmpty()) {
                selectedFilter.set(filters.get(0));
            }
            updateFooter();
        });
        selection.addListener((ListChangeListener<Entry>) c -> updateNameFromSelection());

        addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        modeChanged();
        applyViewMode();
        setDirectory(new File(System.getProperty("user.home", ".")));
    }

    // ------------------------------------------------------------------ building

    private Node buildToolbar() {
        backButton.setGraphic(new Icon(Symbol.BACK, 16));
        forwardButton.setGraphic(new Icon(Symbol.FORWARD, 16));
        upButton.setGraphic(new Icon(Symbol.UP, 16));
        backButton.setTooltip(new Tooltip("Back (Alt+Left)"));
        forwardButton.setTooltip(new Tooltip("Forward (Alt+Right)"));
        upButton.setTooltip(new Tooltip("Up (Alt+Up)"));
        backButton.setOnAction(e -> back());
        forwardButton.setOnAction(e -> forward());
        upButton.setOnAction(e -> up());

        crumbs.getStyleClass().add("tsl-fb-crumbs");
        crumbs.setItemTextProvider(o -> {
            File f = (File) o;
            return f.getName().isEmpty() ? f.getPath() : f.getName();
        });
        crumbs.addItemClickListener((bar, index, item) -> navigate((File) item));
        crumbs.setOnMouseClicked(e -> {
            for (Node n = e.getTarget() instanceof Node ? (Node) e.getTarget() : null; n != null && n != crumbs; n = n.getParent()) {
                if (n instanceof Button) {
                    return; // a crumb handles its own click
                }
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                editAddress();
            }
        });
        pathField.getStyleClass().add("tsl-fb-path");
        pathField.setVisible(false);
        pathField.setOnAction(e -> commitAddress());
        pathField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                endEditAddress();
            }
        });
        pathField.focusedProperty().addListener((o, a, b) -> {
            if (!b) {
                endEditAddress();
            }
        });
        pathField.getProperties().put("tessel.handlesEscape", Boolean.TRUE);
        addressHost.getStyleClass().add("tsl-fb-address");
        addressHost.getChildren().addAll(crumbs, pathField);
        StackPane.setAlignment(crumbs, Pos.CENTER_LEFT);
        HBox.setHgrow(addressHost, Priority.ALWAYS);
        addressHost.setMinWidth(120);
        addressHost.setMaxWidth(Double.MAX_VALUE);
        crumbs.setMinWidth(0);
        javafx.scene.shape.Rectangle addressClip = new javafx.scene.shape.Rectangle();
        addressClip.widthProperty().bind(addressHost.widthProperty());
        addressClip.heightProperty().bind(addressHost.heightProperty());
        addressClip.setArcWidth(12);
        addressClip.setArcHeight(12);
        addressHost.setClip(addressClip);

        Button newFolder = Tessel.style(new Button(), ButtonStyle.ICON);
        newFolder.setGraphic(new Icon(Symbol.NEW_FOLDER, 16));
        newFolder.setTooltip(new Tooltip("New folder (Ctrl+Shift+N)"));
        newFolder.setOnAction(e -> createFolder());

        ToggleGroup group = new ToggleGroup();
        HBox viewButtons = new HBox(2,
                viewToggle(group, Symbol.LIST, "List", ViewMode.LIST),
                viewToggle(group, Symbol.VIEW_DETAILS, "Details", ViewMode.DETAILS),
                viewToggle(group, Symbol.VIEW_ALL, "Tiles", ViewMode.TILES));
        viewButtons.getStyleClass().add("tsl-fb-view-buttons");
        viewMode.addListener((o, a, b) -> {
            for (javafx.scene.control.Toggle t : group.getToggles()) {
                if (t.getUserData() == b) {
                    t.setSelected(true);
                }
            }
        });

        MenuButton more = Tessel.style(new MenuButton(), ButtonStyle.SUBTLE);
        more.setGraphic(new Icon(Symbol.MORE, 16));
        more.getStyleClass().add("tsl-fb-more");
        CheckMenuItem hidden = new CheckMenuItem("Show hidden files");
        hidden.selectedProperty().bindBidirectional(showHidden);
        javafx.scene.control.MenuItem refresh = new javafx.scene.control.MenuItem("Refresh");
        refresh.setOnAction(e -> reload());
        more.getItems().addAll(hidden, refresh);

        HBox bar = new HBox(4, backButton, forwardButton, upButton, addressHost, newFolder, viewButtons, more);
        bar.getStyleClass().add("tsl-fb-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ToggleButton viewToggle(ToggleGroup group, Symbol symbol, String tip, ViewMode target) {
        ToggleButton b = new ToggleButton();
        b.setGraphic(new Icon(symbol, 16));
        b.setToggleGroup(group);
        b.setUserData(target);
        b.getStyleClass().addAll("tsl-icon-button", "tsl-fb-view-toggle");
        b.setTooltip(new Tooltip(tip));
        b.setSelected(viewMode.get() == target);
        b.setOnAction(e -> {
            if (!b.isSelected()) {
                b.setSelected(true); // one view is always chosen
            }
            viewMode.set(target);
        });
        return b;
    }

    private Node buildPlaces() {
        places.getStyleClass().add("tsl-fb-places");
        places.getChildren().add(header("Quick access"));
        File home = new File(System.getProperty("user.home", "."));
        addPlace("Home", Symbol.HOME, home);
        addPlace("Desktop", Symbol.DESKTOP, new File(home, "Desktop"));
        addPlace("Documents", Symbol.DOCUMENT, new File(home, "Documents"));
        addPlace("Downloads", Symbol.DOWNLOAD, new File(home, "Downloads"));
        addPlace("Pictures", Symbol.PICTURES, new File(home, "Pictures"));
        places.getChildren().add(header("This computer"));
        for (File root : File.listRoots()) {
            Label label = addPlace(root.getPath(), Symbol.DRIVE, root);
            // the volume name can take a moment (or hang) for removable drives, so it is read in the background
            Task<String> task = new Task<>() {
                @Override
                protected String call() throws IOException {
                    String volume = Files.getFileStore(root.toPath()).name();
                    return volume == null || volume.isEmpty() ? null : volume;
                }
            };
            task.setOnSucceeded(e -> {
                if (task.getValue() != null) {
                    label.setText(task.getValue() + " (" + root.getPath().replace("\\", "").replace("/", "") + ")");
                }
            });
            Thread t = new Thread(task, "tessel-volume-name");
            t.setDaemon(true);
            t.start();
        }
        ScrollPane scroll = new ScrollPane(places);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("tsl-fb-places-scroll");
        scroll.setPrefWidth(200);
        scroll.setMinWidth(150);
        return scroll;
    }

    private static Label header(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("tsl-fb-place-header", "tsl-semibold");
        return l;
    }

    /** Adds a place if its folder exists; returns the label so a caller can rename it. */
    private Label addPlace(String name, Symbol symbol, File dir) {
        Label label = new Label(name);
        label.getStyleClass().add("tsl-fb-place-label");
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox row = new HBox(10, new Icon(symbol, 16), label);
        row.getStyleClass().add("tsl-fb-place");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setFocusTraversable(true);
        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                navigate(dir);
                row.requestFocus();
            }
        });
        row.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                navigate(dir);
                e.consume();
            }
        });
        placeUpdaters.add(() -> row.pseudoClassStateChanged(SELECTED, dir.equals(directory.get())));
        row.managedProperty().bind(row.visibleProperty());
        row.setVisible(dir.isDirectory());
        places.getChildren().add(row);
        return label;
    }

    private Node buildContent() {
        // details
        TableColumn<Entry, Entry> name = new TableColumn<>("Name");
        name.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        name.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Entry item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : entryNode(item));
            }
        });
        name.setComparator(BY_NAME);
        name.setPrefWidth(340);
        TableColumn<Entry, Entry> modified = textColumn("Date modified", 170, e -> e.modified > 0
                ? DATE.format(Instant.ofEpochMilli(e.modified).atZone(ZoneId.systemDefault())) : "",
                Comparator.comparingLong((Entry e) -> e.modified));
        TableColumn<Entry, Entry> type = textColumn("Type", 130, Entry::type, Comparator.comparing(Entry::type));
        TableColumn<Entry, Entry> size = textColumn("Size", 90, e -> e.size < 0 ? "" : formatSize(e.size),
                Comparator.comparingLong((Entry e) -> e.size));
        size.getStyleClass().add("tsl-fb-size-column");
        table.getColumns().add(name);
        table.getColumns().add(modified);
        table.getColumns().add(type);
        table.getColumns().add(size);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setSortPolicy(tv -> {
            Comparator<Entry> chosen = tv.getComparator();
            FXCollections.sort(entries, DIRECTORIES_FIRST.thenComparing(chosen != null ? chosen : BY_NAME));
            return true;
        });
        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Entry>) c -> viewSelection(table.getSelectionModel().getSelectedItems()));
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Entry> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    activate(row.getItem());
                }
            });
            return row;
        });
        table.setPlaceholder(placeholder);
        table.getStyleClass().add("tsl-fb-table");

        // list
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Entry item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : entryNode(item));
                setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !isEmpty()) {
                        activate(getItem());
                    }
                });
            }
        });
        list.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Entry>) c -> viewSelection(list.getSelectionModel().getSelectedItems()));
        list.getStyleClass().add("tsl-fb-list");

        views.getChildren().addAll(table, list, tiles);
        views.getStyleClass().add("tsl-fb-content");
        return views;
    }

    private TableColumn<Entry, Entry> textColumn(String title, double width, Function<Entry, String> text, Comparator<Entry> sort) {
        TableColumn<Entry, Entry> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Entry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        });
        c.setComparator(sort);
        c.setPrefWidth(width);
        return c;
    }

    private Node buildFooter() {
        nameLabel.getStyleClass().add("tsl-fb-name-label");
        nameField.getStyleClass().add("tsl-fb-name");
        nameField.textProperty().bindBidirectional(fileName);
        nameField.setOnAction(e -> accept());
        HBox.setHgrow(nameField, Priority.ALWAYS);
        filterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExtensionFilter f) {
                return f == null ? "" : f.getDescription();
            }

            @Override
            public ExtensionFilter fromString(String s) {
                return null;
            }
        });
        filterBox.valueProperty().bindBidirectional(selectedFilter);
        filterBox.setPrefWidth(240);
        HBox footer = new HBox(10, nameLabel, nameField, filterBox);
        footer.getStyleClass().add("tsl-fb-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        placeholder.getStyleClass().add("tsl-text-secondary");
        return footer;
    }

    private Node entryNode(Entry e) {
        Icon icon = new Icon(e.directory ? Symbol.FOLDER : Symbol.FILE, 18);
        icon.getStyleClass().add(e.directory ? "tsl-fb-folder-icon" : "tsl-fb-file-icon");
        Label label = new Label(e.name);
        label.getStyleClass().add("tsl-fb-entry-name");
        HBox box = new HBox(8, icon, label);
        box.setAlignment(Pos.CENTER_LEFT);
        if (e.hidden) {
            box.setOpacity(0.55);
        }
        return box;
    }

    // ------------------------------------------------------------------ navigation

    /** Opens a folder (and remembers where you came from). Does nothing when it is not a folder. */
    public boolean navigate(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File old = directory.get();
        if (old != null && !old.equals(dir)) {
            backStack.push(old);
            forwardStack.clear();
        }
        directory.set(dir);
        if (old != null && old.equals(dir)) {
            reload();
        }
        return true;
    }

    public void back() {
        if (!backStack.isEmpty()) {
            forwardStack.push(directory.get());
            directory.set(backStack.pop());
        }
    }

    public void forward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(directory.get());
            directory.set(forwardStack.pop());
        }
    }

    public void up() {
        File parent = directory.get() == null ? null : directory.get().getParentFile();
        if (parent != null) {
            navigate(parent);
        }
    }

    /** Reads the current folder again. */
    public void reload() {
        File dir = directory.get();
        if (dir == null) {
            return;
        }
        final long id = ++loadId;
        final boolean hidden = showHidden.get();
        final ExtensionFilter filter = selectedFilter.get();
        final boolean foldersOnly = mode.get() == Mode.DIRECTORY;
        Task<List<Entry>> task = new Task<>() {
            @Override
            protected List<Entry> call() {
                File[] files = dir.listFiles();
                if (files == null) {
                    throw new IllegalStateException("This folder cannot be opened.");
                }
                List<Entry> result = new ArrayList<>(files.length);
                for (File f : files) {
                    Entry e = new Entry(f);
                    if (e.hidden && !hidden) {
                        continue;
                    }
                    if (!e.directory && (foldersOnly || !matches(filter, e.name))) {
                        continue;
                    }
                    result.add(e);
                }
                result.sort(DIRECTORIES_FIRST.thenComparing(BY_NAME));
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            if (id == loadId) {
                show(task.getValue());
            }
        });
        task.setOnFailed(e -> {
            if (id == loadId) {
                placeholder.setText(task.getException().getMessage() == null
                        ? "This folder cannot be opened." : task.getException().getMessage());
                show(new ArrayList<>());
            }
        });
        Thread t = new Thread(task, "tessel-file-browser");
        t.setDaemon(true);
        t.start();
    }

    private void show(List<Entry> items) {
        syncing = true;
        try {
            entries.setAll(items);
            selection.clear();
            tiles.setItems(items);
        } finally {
            syncing = false;
        }
        if (items.isEmpty() && placeholder.getText().isEmpty()) {
            placeholder.setText("This folder is empty.");
        }
        if (pendingSelect != null) {
            for (Entry e : items) {
                if (e.file.equals(pendingSelect)) {
                    selectEntries(List.of(e));
                    break;
                }
            }
            pendingSelect = null;
        }
        if (!table.getSortOrder().isEmpty()) {
            table.sort();
        }
    }

    private static boolean matches(ExtensionFilter filter, String name) {
        if (filter == null) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String pattern : filter.getExtensions()) {
            String p = pattern.toLowerCase(Locale.ROOT);
            if (p.equals("*") || p.equals("*.*")) {
                return true;
            }
            if (p.startsWith("*") && lower.endsWith(p.substring(1))) {
                return true;
            }
            if (p.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private void directoryChanged() {
        File dir = directory.get();
        placeholder.setText("");
        // the address bar: every folder from the root down to this one
        List<Object> chain = new ArrayList<>();
        for (File f = dir; f != null; f = f.getParentFile()) {
            chain.add(0, f);
        }
        crumbs.getItems().setAll(chain);
        backButton.setDisable(backStack.isEmpty());
        forwardButton.setDisable(forwardStack.isEmpty());
        upButton.setDisable(dir == null || dir.getParentFile() == null);
        for (Runnable r : placeUpdaters) {
            r.run();
        }
        if (mode.get() == Mode.DIRECTORY) {
            fileName.set("");
        }
        reload();
    }

    // ------------------------------------------------------------------ address bar

    private void editAddress() {
        pathField.setText(directory.get() == null ? "" : directory.get().getPath());
        crumbs.setVisible(false);
        addressHost.pseudoClassStateChanged(EDITING, true);
        pathField.setVisible(true);
        pathField.requestFocus();
        pathField.selectAll();
    }

    private void endEditAddress() {
        if (pathField.isVisible()) {
            pathField.setVisible(false);
            addressHost.pseudoClassStateChanged(EDITING, false);
            crumbs.setVisible(true);
            (viewMode.get() == ViewMode.LIST ? list : table).requestFocus();
        }
    }

    private void commitAddress() {
        File typed = new File(pathField.getText().trim());
        if (typed.isDirectory()) {
            endEditAddress();
            navigate(typed);
        } else if (typed.isFile() && typed.getParentFile() != null) {
            endEditAddress();
            pendingSelect = typed;
            if (!navigate(typed.getParentFile())) {
                pendingSelect = null;
            } else if (typed.getParentFile().equals(directory.get())) {
                reload();
            }
        } else {
            flash(pathField);
        }
    }

    // ------------------------------------------------------------------ selection

    private void viewSelection(List<Entry> selected) {
        if (syncing) {
            return;
        }
        selectEntries(new ArrayList<>(selected));
    }

    private void selectEntries(List<Entry> selected) {
        syncing = true;
        try {
            List<Entry> keep = mode.get() == Mode.OPEN_MULTIPLE || selected.size() <= 1
                    ? selected : List.of(selected.get(selected.size() - 1));
            selection.setAll(keep);
            pushSelection(table.getSelectionModel(), keep);
            pushSelection(list.getSelectionModel(), keep);
            tiles.select(keep);
        } finally {
            syncing = false;
        }
    }

    private void pushSelection(javafx.scene.control.MultipleSelectionModel<Entry> model, List<Entry> keep) {
        if (new ArrayList<>(model.getSelectedItems()).equals(keep)) {
            return;
        }
        model.clearSelection();
        for (Entry e : keep) {
            model.select(e);
        }
    }

    private void updateNameFromSelection() {
        if (selection.isEmpty()) {
            return;
        }
        Mode m = mode.get();
        if (m == Mode.DIRECTORY) {
            Entry e = selection.get(0);
            fileName.set(e.directory ? e.name : "");
        } else if (m == Mode.SAVE) {
            Entry e = selection.get(0);
            if (!e.directory) {
                fileName.set(e.name);
            }
        } else {
            List<String> names = new ArrayList<>();
            for (Entry e : selection) {
                if (!e.directory) {
                    names.add(e.name);
                }
            }
            if (names.size() == 1) {
                fileName.set(names.get(0));
            } else if (!names.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String n : names) {
                    sb.append(sb.length() == 0 ? "" : " ").append('"').append(n).append('"');
                }
                fileName.set(sb.toString());
            }
        }
    }

    private void activate(Entry e) {
        if (e == null) {
            return;
        }
        if (e.directory) {
            navigate(e.file);
        } else {
            selectEntries(List.of(e));
            accept();
        }
    }

    private void accept() {
        Runnable r = onAccept.get();
        if (r != null) {
            r.run();
        }
    }

    // ------------------------------------------------------------------ result

    /**
     * What the user chose, for the current mode: the files to open, the file to write, or the folder.
     * Empty when nothing valid is chosen yet. A typed folder name opens that folder instead (and the result is empty);
     * an invalid name makes the name field flash.
     */
    public List<File> resolveResult() {
        String typed = fileName.get().trim();
        File dir = directory.get();
        switch (mode.get()) {
            case DIRECTORY:
                if (!selection.isEmpty() && selection.get(0).directory) {
                    return List.of(selection.get(0).file);
                }
                if (!typed.isEmpty()) {
                    File f = resolve(typed);
                    if (f.isDirectory()) {
                        return List.of(f);
                    }
                    flash(nameField);
                    return List.of();
                }
                return dir == null ? List.of() : List.of(dir);
            case SAVE: {
                if (typed.isEmpty()) {
                    flash(nameField);
                    return List.of();
                }
                File f = resolve(typed);
                if (f.isDirectory()) {
                    fileName.set("");
                    navigate(f);
                    return List.of();
                }
                return List.of(withDefaultExtension(f));
            }
            default: {
                List<File> result = new ArrayList<>();
                List<String> names = names(typed);
                if (names.isEmpty()) {
                    for (Entry e : selection) {
                        if (!e.directory) {
                            result.add(e.file);
                        }
                    }
                    if (result.isEmpty()) {
                        flash(nameField);
                    }
                    return result;
                }
                for (String n : names) {
                    File f = resolve(n);
                    if (f.isDirectory()) {
                        fileName.set("");
                        navigate(f);
                        return List.of();
                    }
                    if (!f.isFile()) {
                        flash(nameField);
                        return List.of();
                    }
                    result.add(f);
                }
                if (mode.get() == Mode.OPEN && result.size() > 1) {
                    return List.of(result.get(0));
                }
                return result;
            }
        }
    }

    private static List<String> names(String typed) {
        List<String> names = new ArrayList<>();
        if (typed.isEmpty()) {
            return names;
        }
        if (!typed.contains("\"")) {
            names.add(typed);
            return names;
        }
        Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(typed);
        while (m.find()) {
            if (!m.group(1).isEmpty()) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    private File resolve(String name) {
        File f = new File(name);
        return f.isAbsolute() || directory.get() == null ? f : new File(directory.get(), name);
    }

    /** "report" with the filter "*.txt" becomes "report.txt"; names that already have an extension are kept. */
    private File withDefaultExtension(File f) {
        ExtensionFilter filter = selectedFilter.get();
        if (filter == null || f.getName().contains(".")) {
            return f;
        }
        String single = null;
        for (String p : filter.getExtensions()) {
            if (p.startsWith("*.") && p.length() > 2 && !p.equals("*.*")) {
                if (single != null) {
                    return f; // several extensions: no obvious default
                }
                single = p.substring(1);
            }
        }
        return single == null ? f : new File(f.getParentFile(), f.getName() + single);
    }

    private void flash(TextInputControl field) {
        field.getStyleClass().add("tsl-invalid");
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> field.getStyleClass().remove("tsl-invalid"));
        pause.play();
    }

    // ------------------------------------------------------------------ new folder

    /** Asks for a name and creates a folder in the current directory. */
    public void createFolder() {
        File dir = directory.get();
        if (dir == null || getScene() == null) {
            return;
        }
        TextField name = new TextField("New folder");
        VBox body = new VBox(8, new TextBlock("Name of the new folder"), name);
        ContentDialog dialog = new ContentDialog("New folder", body);
        dialog.setPrimaryButtonText("Create");
        dialog.setCloseButtonText("Cancel");
        dialog.primaryButtonDisabledProperty().bind(name.textProperty().isEmpty());
        dialog.show(this).thenAccept(result -> Platform.runLater(() -> {
            if (result != ContentDialog.Result.PRIMARY) {
                return;
            }
            File created = new File(dir, name.getText().trim());
            try {
                Files.createDirectory(created.toPath());
                pendingSelect = created;
                reload();
            } catch (IOException | RuntimeException ex) {
                Snackbar.show(this, "Cannot create the folder", ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                        Severity.ERROR);
            }
        }));
        Platform.runLater(() -> {
            name.requestFocus();
            name.selectAll();
        });
    }

    // ------------------------------------------------------------------ misc

    private void onKey(KeyEvent e) {
        boolean typing = e.getTarget() instanceof TextInputControl;
        if (e.getCode() == KeyCode.LEFT && e.isAltDown()) {
            back();
        } else if (e.getCode() == KeyCode.RIGHT && e.isAltDown()) {
            forward();
        } else if (e.getCode() == KeyCode.UP && e.isAltDown()) {
            up();
        } else if (e.getCode() == KeyCode.BACK_SPACE && !typing) {
            up();
        } else if (e.getCode() == KeyCode.F5) {
            reload();
        } else if (e.getCode() == KeyCode.L && e.isControlDown()) {
            editAddress();
        } else if (e.getCode() == KeyCode.N && e.isControlDown() && e.isShiftDown()) {
            createFolder();
        } else if (e.getCode() == KeyCode.ENTER && !typing && selection.size() == 1 && selection.get(0).directory
                && !(e.getTarget() instanceof Button)) {
            navigate(selection.get(0).file);
        } else {
            return;
        }
        e.consume();
    }

    private void modeChanged() {
        Mode m = mode.get();
        boolean multiple = m == Mode.OPEN_MULTIPLE;
        table.getSelectionModel().setSelectionMode(multiple ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        list.getSelectionModel().setSelectionMode(multiple ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        tiles.multiple = multiple;
        updateFooter();
        if (directory.get() != null) {
            reload();
        }
    }

    private void updateFooter() {
        Mode m = mode.get();
        nameLabel.setText(m == Mode.DIRECTORY ? "Folder:" : "File name:");
        nameField.setPromptText(m == Mode.SAVE ? "Name of the file" : m == Mode.DIRECTORY ? "Name of the folder" : "");
        boolean showFilter = m != Mode.DIRECTORY && !filters.isEmpty();
        filterBox.setVisible(showFilter);
        filterBox.setManaged(showFilter);
    }

    private void applyViewMode() {
        ViewMode v = viewMode.get();
        table.setVisible(v == ViewMode.DETAILS);
        list.setVisible(v == ViewMode.LIST);
        tiles.setVisible(v == ViewMode.TILES);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double v = bytes / 1024.0;
        String[] units = {"KB", "MB", "GB", "TB"};
        int i = 0;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format(Locale.ROOT, v < 10 ? "%.1f %s" : "%.0f %s", v, units[i]);
    }

    // ------------------------------------------------------------------ tiles

    /** The tile view: big icons with names, wrapped. */
    private final class Tiles extends ScrollPane {
        private final FlowPane flow = new FlowPane(8, 8);
        private final List<Entry> items = new ArrayList<>();
        private final List<VBox> nodes = new ArrayList<>();
        boolean multiple;
        private Entry anchor;

        Tiles() {
            getStyleClass().add("tsl-fb-tiles");
            flow.getStyleClass().add("tsl-fb-tiles-flow");
            setContent(flow);
            setFitToWidth(true);
            setHbarPolicy(ScrollBarPolicy.NEVER);
        }

        void setItems(List<Entry> entries) {
            items.clear();
            items.addAll(entries);
            nodes.clear();
            flow.getChildren().clear();
            anchor = null;
            for (Entry e : entries) {
                Icon icon = new Icon(e.directory ? Symbol.FOLDER : Symbol.FILE, 36);
                icon.getStyleClass().add(e.directory ? "tsl-fb-folder-icon" : "tsl-fb-file-icon");
                Label label = new Label(e.name);
                label.setWrapText(true);
                label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                label.setAlignment(Pos.TOP_CENTER);
                label.setMaxWidth(96);
                label.setMaxHeight(40);
                label.getStyleClass().add("tsl-fb-tile-name");
                VBox tile = new VBox(6, icon, label);
                tile.setAlignment(Pos.TOP_CENTER);
                tile.getStyleClass().add("tsl-fb-tile");
                tile.setPrefSize(104, 92);
                tile.setMinSize(104, 92);
                if (e.hidden) {
                    tile.setOpacity(0.55);
                }
                tile.setOnMouseClicked(ev -> {
                    if (ev.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    if (ev.getClickCount() == 2) {
                        activate(e);
                    } else {
                        click(e, ev.isControlDown(), ev.isShiftDown());
                    }
                });
                nodes.add(tile);
                flow.getChildren().add(tile);
            }
        }

        private void click(Entry e, boolean ctrl, boolean shift) {
            List<Entry> next = new ArrayList<>();
            if (multiple && shift && anchor != null) {
                int a = items.indexOf(anchor);
                int b = items.indexOf(e);
                for (int i = Math.min(a, b); i <= Math.max(a, b); i++) {
                    next.add(items.get(i));
                }
            } else if (multiple && ctrl) {
                next.addAll(selection);
                if (!next.remove(e)) {
                    next.add(e);
                }
                anchor = e;
            } else {
                next.add(e);
                anchor = e;
            }
            selectEntries(next);
        }

        void select(List<Entry> selected) {
            for (int i = 0; i < items.size(); i++) {
                nodes.get(i).pseudoClassStateChanged(SELECTED, selected.contains(items.get(i)));
            }
        }
    }

    // ------------------------------------------------------------------ properties

    public final Mode getMode() {
        return mode.get();
    }

    public final void setMode(Mode mode) {
        this.mode.set(mode);
    }

    public final ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    public final ViewMode getViewMode() {
        return viewMode.get();
    }

    public final void setViewMode(ViewMode viewMode) {
        this.viewMode.set(viewMode);
    }

    public final ObjectProperty<ViewMode> viewModeProperty() {
        return viewMode;
    }

    /** The folder that is shown. Setting a path that is not a folder is ignored. */
    public final File getDirectory() {
        return directory.get();
    }

    public final void setDirectory(File dir) {
        navigate(dir);
    }

    public final ObjectProperty<File> directoryProperty() {
        return directory;
    }

    public final boolean isShowHidden() {
        return showHidden.get();
    }

    public final void setShowHidden(boolean showHidden) {
        this.showHidden.set(showHidden);
    }

    public final BooleanProperty showHiddenProperty() {
        return showHidden;
    }

    /** The text of the name field: the chosen file name(s), or the name to save as. */
    public final String getFileName() {
        return fileName.get();
    }

    public final void setFileName(String name) {
        fileName.set(name == null ? "" : name);
    }

    public final StringProperty fileNameProperty() {
        return fileName;
    }

    /** The filters offered in the combo box next to the name field (none: all files, no combo box). */
    public final ObservableList<ExtensionFilter> getFilters() {
        return filters;
    }

    public final ExtensionFilter getSelectedFilter() {
        return selectedFilter.get();
    }

    public final void setSelectedFilter(ExtensionFilter filter) {
        selectedFilter.set(filter);
    }

    public final ObjectProperty<ExtensionFilter> selectedFilterProperty() {
        return selectedFilter;
    }

    /** Runs when the user confirms with a double click on a file or Enter in the name field. */
    public final void setOnAccept(Runnable onAccept) {
        this.onAccept.set(onAccept);
    }

    public final Runnable getOnAccept() {
        return onAccept.get();
    }

    /** The currently selected files and folders. */
    public final List<File> getSelectedFiles() {
        List<File> files = new ArrayList<>();
        for (Entry e : selection) {
            files.add(e.file);
        }
        return files;
    }
}
