package net.tessel.fx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * Entry point of Tessel for JavaFX: installs the theme, switches between light, dark and system themes at runtime,
 * and offers a few styling helpers.
 *
 * <pre>{@code
 * public void start(Stage stage) {
 *     Tessel.setup(TesselTheme.SYSTEM);          // or LIGHT / DARK; call it before the first window is shown
 *     stage.setScene(new Scene(root));
 *     stage.show();
 * }
 * }</pre>
 *
 * Once installed, every scene of every window (including pop-ups, context menus and dialogs) is themed
 * automatically, and follows {@link #setTheme(TesselTheme)} and {@link #setAccent(Color)}. Call {@link #apply(Scene)}
 * to theme a scene explicitly, or put {@link #IGNORE} in its properties to leave it alone.
 *
 * <p>Methods that change the theme must be called on the JavaFX application thread (they are forwarded there when
 * called from another thread).
 */
public final class Tessel {

    public static final String VERSION = "1.0.0";

    /** Scene property: when present the scene is not themed automatically. */
    public static final String IGNORE = "net.tessel.fx.ignore";

    /** The style class of a scene root that carries the theme. */
    public static final String ROOT_CLASS = "tessel-root";

    /** The style class of the root of a window's scene (not of a pop-up); it paints the page background. */
    public static final String WINDOW_ROOT_CLASS = "tessel-window-root";

    private static final String SHEET_KEY = "net.tessel.fx.sheet";
    private static final String HOOK_KEY = "net.tessel.fx.hooked";
    private static final String DIALOG_KEY = "net.tessel.fx.dialog";
    private static final PseudoClass KEYBOARD = PseudoClass.getPseudoClass("keyboard");

    private static final ObjectProperty<Palette> PALETTE = new SimpleObjectProperty<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static TesselTheme theme = TesselTheme.SYSTEM;
    private static Color accent;
    private static String sheetUri;
    private static boolean installed;
    private static boolean watchingSystem;
    private static String rules;

    private Tessel() {
    }

    // ------------------------------------------------------------------ setup

    /** Installs Tessel with the system theme and the default accent. */
    public static void setup() {
        setup(TesselTheme.SYSTEM, null);
    }

    public static void setup(TesselTheme theme) {
        setup(theme, null);
    }

    /**
     * @param theme  the color theme
     * @param accent a custom accent color, or null for the built-in indigo
     */
    public static void setup(TesselTheme theme, Color accent) {
        Tessel.theme = theme == null ? TesselTheme.SYSTEM : theme;
        Tessel.accent = accent;
        onFx(Tessel::refresh);
    }

    /** Changes the theme; every open window is updated. */
    public static void setTheme(TesselTheme theme) {
        Tessel.theme = theme == null ? TesselTheme.SYSTEM : theme;
        onFx(Tessel::refresh);
    }

    /** Changes the accent color; null goes back to the built-in indigo. */
    public static void setAccent(Color accent) {
        Tessel.accent = accent;
        onFx(Tessel::refresh);
    }

    /** Uses the accent color of the operating system (Windows) or the default accent where it cannot be read. */
    public static void useSystemAccent() {
        setAccent(getSystemAccent());
    }

    public static TesselTheme getTheme() {
        return theme;
    }

    /** The custom accent color, or null for the built-in one. */
    public static Color getAccent() {
        return accent;
    }

    /** The colors of the active theme. */
    public static Palette palette() {
        Palette p = PALETTE.get();
        if (p == null) {
            p = new Palette(resolveDark(), accent);
            PALETTE.set(p);
        }
        return p;
    }

    /**
     * The active palette as an observable value, for custom drawing code that has to repaint on theme changes.
     * Controls that are styled with CSS do not need it.
     */
    public static ObjectProperty<Palette> paletteProperty() {
        palette();
        return PALETTE;
    }

    public static boolean isDark() {
        return palette().dark;
    }

    /** Runs the listener after every theme or accent change. */
    public static void addThemeListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeThemeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    /** The URL of the current generated style sheet (a {@code data:} URI). */
    public static String stylesheet() {
        if (sheetUri == null) {
            sheetUri = buildSheet(palette());
        }
        return sheetUri;
    }

    // ------------------------------------------------------------------ applying

    /** Themes a scene, and keeps it themed. Scenes of shown windows are themed automatically. */
    public static void apply(Scene scene) {
        if (scene == null || scene.getProperties().containsKey(IGNORE)) {
            return;
        }
        ensureInstalled();
        String uri = stylesheet();
        ObservableList<String> sheets = scene.getStylesheets();
        Object old = scene.getProperties().put(SHEET_KEY, uri);
        if (old != null && !old.equals(uri)) {
            sheets.remove(old);
        }
        if (!sheets.contains(uri)) {
            sheets.add(0, uri);
        }
        styleRoot(scene.getRoot(), scene);
        if (scene.getRoot() instanceof DialogPane) {
            decorateDialog((DialogPane) scene.getRoot(), scene);
        }
        if (scene.getProperties().putIfAbsent(HOOK_KEY, Boolean.TRUE) == null) {
            scene.rootProperty().addListener((o, oldRoot, newRoot) -> {
                if (oldRoot != null) {
                    oldRoot.getStyleClass().removeAll(ROOT_CLASS, "tessel-dark", "tessel-light", WINDOW_ROOT_CLASS);
                }
                styleRoot(newRoot, scene);
            });
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (isNavigationKey(e.getCode())) {
                    setKeyboardMode(scene.getRoot(), true);
                }
            });
            scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> setKeyboardMode(scene.getRoot(), false));
        }
    }

    /** Themes the scene of a window (now, or as soon as it has one). */
    public static void apply(Window window) {
        if (window == null) {
            return;
        }
        track(window);
    }

    private static void styleRoot(Parent root, Scene scene) {
        if (root == null) {
            return;
        }
        ObservableList<String> classes = root.getStyleClass();
        if (!classes.contains(ROOT_CLASS)) {
            classes.add(ROOT_CLASS);
        }
        // the roots of windows get the page background; the roots of pop-ups (menus, tooltips) stay transparent
        if (!(scene.getWindow() instanceof PopupWindow) && !classes.contains(WINDOW_ROOT_CLASS)) {
            classes.add(WINDOW_ROOT_CLASS);
        }
        boolean dark = palette().dark;
        classes.removeAll(dark ? "tessel-light" : "tessel-dark");
        if (!classes.contains(dark ? "tessel-dark" : "tessel-light")) {
            classes.add(dark ? "tessel-dark" : "tessel-light");
        }
    }

    /**
     * Gives a standard {@code Alert} / {@code Dialog} the Tessel look beyond the style sheet: its blue Modena picture
     * becomes a vector icon, and the window is fitted again when Tessel was applied after the dialog had been sized.
     * Runs once per dialog scene.
     */
    private static void decorateDialog(DialogPane pane, Scene scene) {
        if (scene.getProperties().putIfAbsent(DIALOG_KEY, Boolean.TRUE) != null) {
            return;
        }
        ObservableList<String> classes = pane.getStyleClass();
        Icon icon = null;
        if (classes.contains("alert")) {
            Severity severity = classes.contains("error") ? Severity.ERROR
                    : classes.contains("warning") ? Severity.WARNING
                    : classes.contains("information") ? Severity.INFORMATIONAL : null;
            icon = severity != null ? new Icon(severity.iconKey(), 32) : questionIcon();
            if (severity != null) {
                icon.getStyleClass().add(severity.styleClass());
            }
        } else if (classes.contains("text-input-dialog") || classes.contains("choice-dialog")) {
            icon = questionIcon();
        }
        if (icon != null && (pane.getGraphic() == null || isModenaDialogImage(pane.getGraphic()))) {
            icon.getStyleClass().add("tsl-dialog-icon");
            pane.setGraphic(icon);
        }
        Window window = scene.getWindow();
        if (window instanceof Stage) {
            Stage stage = (Stage) window;
            double cx = stage.getX() + stage.getWidth() / 2;
            double cy = stage.getY() + stage.getHeight() / 2;
            pane.applyCss();
            stage.sizeToScene();
            if (!Double.isNaN(cx) && !Double.isNaN(cy)) {
                stage.setX(cx - stage.getWidth() / 2);
                stage.setY(cy - stage.getHeight() / 2);
            }
        }
    }

    /** True for the pictures the built-in theme puts on alerts (dialog-confirm.png, ...), but not for your own graphic. */
    private static boolean isModenaDialogImage(Node graphic) {
        if (graphic instanceof ImageView) {
            Image image = ((ImageView) graphic).getImage();
            return image != null && image.getUrl() != null && image.getUrl().contains("dialog-");
        }
        return false;
    }

    private static Icon questionIcon() {
        Icon icon = new Icon(Symbol.HELP, 32);
        icon.getStyleClass().add("tsl-question");
        return icon;
    }

    private static void setKeyboardMode(Parent root, boolean on) {
        if (root != null) {
            root.pseudoClassStateChanged(KEYBOARD, on);
        }
    }

    private static boolean isNavigationKey(KeyCode code) {
        switch (code) {
            case TAB: case UP: case DOWN: case LEFT: case RIGHT: case SPACE: case ENTER: case HOME: case END:
                return true;
            default:
                return false;
        }
    }

    private static void ensureInstalled() {
        if (installed) {
            return;
        }
        installed = true;
        Window.getWindows().addListener((ListChangeListener<Window>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Window w : new ArrayList<>(c.getAddedSubList())) {
                        track(w);
                    }
                }
            }
        });
        for (Window w : new ArrayList<>(Window.getWindows())) {
            track(w);
        }
    }

    private static void track(Window window) {
        if (window.getProperties().putIfAbsent(HOOK_KEY, Boolean.TRUE) == null) {
            window.sceneProperty().addListener((o, oldScene, scene) -> apply(scene));
        }
        if (window.getScene() != null) {
            apply(window.getScene());
        }
    }

    private static void refresh() {
        ensureInstalled();
        Palette p = new Palette(resolveDark(), accent);
        sheetUri = buildSheet(p);
        PALETTE.set(p);
        for (Window w : new ArrayList<>(Window.getWindows())) {
            if (w.getScene() != null) {
                apply(w.getScene());
            }
        }
        for (Runnable r : LISTENERS) {
            r.run();
        }
        if (theme == TesselTheme.SYSTEM) {
            watchSystemScheme();
        }
    }

    private static void onFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        try {
            Platform.runLater(action);
        } catch (IllegalStateException toolkitNotRunning) {
            // called before the toolkit started (for example from main): the state is kept, but nothing
            // installs the theme on windows until Tessel.setup / Tessel.apply runs again on the FX thread
            PALETTE.set(null);
            sheetUri = null;
            System.err.println("Tessel: setup() ran before the JavaFX toolkit started. "
                    + "Call Tessel.setup() from Application.start() (or init()) so windows are themed.");
        }
    }

    // ------------------------------------------------------------------ style sheet

    private static String buildSheet(Palette p) {
        StringBuilder css = new StringBuilder(96 * 1024);
        // Pop-ups (menus, tool tips, combo box lists) are styled once before they are linked to their owner window, so
        // their roots get the palette themselves instead of relying on a .tessel-root ancestor.
        // A Dialog copies its owner's style sheets into its own scene before Tessel can mark the root, so the root of a
        // dialog needs them as well (otherwise its window is sized with the default font and the buttons get cut).
        css.append(".tessel-root, .context-menu, .tooltip, .combo-box-popup, .dialog-pane {\n").append(p.variables());
        css.append("  -fx-font-family: \"").append(Fonts.uiFamily()).append("\";\n");
        css.append("  -fx-font-size: 14px;\n}\n");
        String semibold = Fonts.semiboldFamily();
        css.append(".tsl-semibold { ");
        css.append(semibold != null ? "-fx-font-family: \"" + semibold + "\";" : "-fx-font-weight: bold;");
        css.append(" }\n");
        css.append(".tsl-mono { -fx-font-family: \"").append(Fonts.monoFamily()).append("\"; }\n");
        css.append(rules());
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static synchronized String rules() {
        if (rules == null) {
            try (InputStream in = Tessel.class.getResourceAsStream("tessel.css")) {
                if (in == null) {
                    throw new IllegalStateException("tessel.css is missing from the Tessel jar");
                }
                rules = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read tessel.css", e);
            }
        }
        return rules;
    }

    // ------------------------------------------------------------------ styling helpers

    /** Gives a button one of the {@link ButtonStyle} variants. Works for buttons, toggle buttons and menu buttons. */
    public static <T extends ButtonBase> T style(T button, ButtonStyle style) {
        for (ButtonStyle s : ButtonStyle.values()) {
            if (s.styleClass() != null) {
                button.getStyleClass().remove(s.styleClass());
            }
        }
        if (style != null && style.styleClass() != null) {
            button.getStyleClass().add(style.styleClass());
        }
        return button;
    }

    /** Sets the {@link ProgressState} of a progress bar, progress indicator or {@code ProgressRing}. */
    public static <T extends Node> T progressState(T node, ProgressState state) {
        for (ProgressState s : ProgressState.values()) {
            if (s.styleClass() != null) {
                node.getStyleClass().remove(s.styleClass());
            }
        }
        if (state != null && state.styleClass() != null) {
            node.getStyleClass().add(state.styleClass());
        }
        if (node instanceof ProgressBar) {
            ProgressBar bar = (ProgressBar) node;
            Object saved = bar.getProperties().get("net.tessel.fx.indeterminate");
            if (state != null && state.isFrozen() && bar.isIndeterminate()) {
                bar.getProperties().put("net.tessel.fx.indeterminate", Boolean.TRUE);
                bar.setProgress(state == ProgressState.PAUSED ? 0.5 : 1.0);
            } else if ((state == null || !state.isFrozen()) && saved != null) {
                bar.getProperties().remove("net.tessel.fx.indeterminate");
                bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            }
        }
        return node;
    }

    /** Applies a {@link TextStyle} (font size, weight and color) to a label, text or any other node. */
    public static <T extends Node> T textStyle(T node, TextStyle style) {
        for (TextStyle s : TextStyle.values()) {
            node.getStyleClass().remove(s.styleClass());
        }
        node.getStyleClass().remove("tsl-semibold");
        if (style != null) {
            node.getStyleClass().add(style.styleClass());
            if (style.isSemibold()) {
                node.getStyleClass().add("tsl-semibold");
            }
            if (style == TextStyle.CODE) {
                node.getStyleClass().add("tsl-mono");
            }
        }
        return node;
    }

    /** A label with a {@link TextStyle}. */
    public static Label label(String text, TextStyle style) {
        return textStyle(new Label(text), style);
    }

    /**
     * Puts an icon at the start of a text field. Add the returned node to the layout instead of the field:
     * the field is wrapped in a stack pane that also draws the icon.
     */
    public static StackPane withLeadingIcon(TextInputControl field, Symbol symbol) {
        field.getStyleClass().add("tsl-leading-icon");
        Icon icon = new Icon(symbol, 14);
        icon.setColor(null);
        icon.getStyleClass().add("tsl-field-icon");
        StackPane pane = new StackPane(field, icon);
        pane.getStyleClass().add("tsl-icon-field");
        StackPane.setAlignment(icon, javafx.geometry.Pos.CENTER_LEFT);
        StackPane.setMargin(icon, new javafx.geometry.Insets(0, 0, 0, 10));
        return pane;
    }

    /** The font of a text style, for custom drawing. */
    public static Font font(TextStyle style) {
        return Fonts.forStyle(style);
    }

    // ------------------------------------------------------------------ system integration

    private static boolean resolveDark() {
        switch (theme) {
            case DARK:
                return true;
            case LIGHT:
                return false;
            default:
                return isSystemDark();
        }
    }

    /** True when the operating system uses a dark color scheme for apps. */
    public static boolean isSystemDark() {
        Boolean fx = javafxColorScheme();
        if (fx != null) {
            return fx;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                String out = run("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme");
                return out != null && out.matches("(?s).*AppsUseLightTheme\\s+REG_DWORD\\s+0x0\\s*.*");
            }
            if (os.contains("mac")) {
                String out = run("defaults", "read", "-g", "AppleInterfaceStyle");
                return out != null && out.trim().equalsIgnoreCase("Dark");
            }
            String out = run("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
            return out != null && out.contains("dark");
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The accent color of the operating system (Windows only), or null when it cannot be read. */
    public static Color getSystemAccent() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return null;
        }
        String out = run("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\DWM", "/v", "AccentColor");
        if (out == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("AccentColor\\s+REG_DWORD\\s+0x([0-9a-fA-F]+)").matcher(out);
        if (!m.find()) {
            return null;
        }
        long v = Long.parseLong(m.group(1), 16);
        return Color.rgb((int) (v & 0xFF), (int) ((v >> 8) & 0xFF), (int) ((v >> 16) & 0xFF));
    }

    /** JavaFX 22+ reports the color scheme itself; older versions return null. Reflection keeps 21 working. */
    private static Boolean javafxColorScheme() {
        try {
            Method getPreferences = Platform.class.getMethod("getPreferences");
            Object prefs = getPreferences.invoke(null);
            Class<?> type = Class.forName("javafx.application.Platform$Preferences");
            Object scheme = type.getMethod("getColorScheme").invoke(prefs);
            return scheme != null && scheme.toString().equals("DARK");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void watchSystemScheme() {
        if (watchingSystem) {
            return;
        }
        try {
            Method getPreferences = Platform.class.getMethod("getPreferences");
            Object prefs = getPreferences.invoke(null);
            Class<?> type = Class.forName("javafx.application.Platform$Preferences");
            ObservableValue<Object> scheme = (ObservableValue<Object>) type.getMethod("colorSchemeProperty").invoke(prefs);
            scheme.addListener((o, a, b) -> {
                if (theme == TesselTheme.SYSTEM) {
                    refresh();
                }
            });
            watchingSystem = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            watchingSystem = true; // not supported by this JavaFX version
        }
    }

    private static String run(String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return p.exitValue() == 0 ? sb.toString() : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
