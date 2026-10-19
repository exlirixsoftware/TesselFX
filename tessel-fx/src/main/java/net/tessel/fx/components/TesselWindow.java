package net.tessel.fx.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import net.tessel.fx.Tessel;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * A window with Tessel's own title bar: the icon, the title, any components you add, and minimize / maximize / close
 * buttons, inside a rounded, shadowed frame. It is dragged by the title bar, resized from its edges, and maximized by
 * double-clicking the title bar.
 *
 * <pre>{@code
 * TesselWindow window = new TesselWindow("My App");
 * window.getIcons().add(logo);
 * window.addTitleBarComponent(new Button("..."));
 * window.setContent(new NavigationView("My App"));
 * window.setContentSize(1100, 720);
 * window.show();
 * }</pre>
 *
 * It is a {@link Stage}, so the usual window API ({@code setTitle}, {@code setOnCloseRequest}, {@code initOwner}, ...)
 * works. {@link #setMaximized(boolean)} and {@link #setFullScreen(boolean)} are not used; call
 * {@link #toggleMaximize()}. Size and minimum size are those of the content ({@link #setContentSize},
 * {@link #setContentMinSize}); the frame adds a margin for the shadow.
 *
 * <p>Unlike Tessel4J's {@code TesselFrame} it does not hook into the native window procedure, so Windows 11 Snap
 * Layouts and Aero Snap are not available; dragging, resizing and maximizing are done in JavaFX.
 */
public class TesselWindow extends Stage {

    /** Transparent margin around the frame that holds the shadow. */
    static final double SHADOW = 14;
    private static final double RESIZE_ZONE = 6;
    private static final double TITLE_HEIGHT = 36;
    private static final PseudoClass MAXIMIZED = PseudoClass.getPseudoClass("maximized");
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private final BooleanProperty windowMaximized = new SimpleBooleanProperty(false);
    private final DoubleProperty iconSize = new SimpleDoubleProperty(16);

    private final StackPane shell = new StackPane();
    private final StackPane frame = new StackPane();
    private final StackPane clipped = new StackPane();
    private final BorderPane main = new BorderPane();
    private final HBox titleBar = new HBox();
    private final HBox titleComponents = new HBox(4);
    private final ImageView iconView = new ImageView();
    private final StackPane contentHost = new StackPane();
    private final StackPane overlay = new StackPane();
    private final Button minimizeButton = new Button();
    private final Button maximizeButton = new Button();
    private final Button closeButton = new Button();
    private final Rectangle clip = new Rectangle();
    private final Scene scene;

    private Rectangle2D restoreBounds;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean titleDragging;

    // resizing
    private int edge;
    private double startX;
    private double startY;
    private double startW;
    private double startH;
    private double startMouseX;
    private double startMouseY;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int TOP = 4;
    private static final int BOTTOM = 8;

    public TesselWindow() {
        this("");
    }

    public TesselWindow(String title) {
        this(title, false);
    }

    /** @param dialog true for a window without minimize and maximize buttons (used by {@link TesselDialog}) */
    TesselWindow(String title, boolean dialog) {
        super(StageStyle.TRANSPARENT);
        setTitle(title);

        shell.getStyleClass().add("tsl-window-shell");
        frame.getStyleClass().add("tsl-window-frame");
        clipped.getStyleClass().add("tsl-window-clip");

        buildTitleBar(dialog);
        main.setTop(titleBar);
        contentHost.getStyleClass().add("tsl-window-content");
        main.setCenter(contentHost);

        overlay.getStyleClass().add("tsl-overlay-layer");
        overlay.setPickOnBounds(false);
        clipped.getChildren().addAll(main, overlay);
        frame.getChildren().add(clipped);
        shell.getChildren().add(frame);

        clip.widthProperty().bind(clipped.widthProperty());
        clip.heightProperty().bind(clipped.heightProperty());
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        clipped.setClip(clip);

        // a dialog is packed to its content (sizeToScene); a window starts at a default size until setContentSize
        scene = new Scene(shell, dialog ? -1 : 600, dialog ? -1 : 400, Color.TRANSPARENT);
        scene.getProperties().put(Overlays.LAYER_KEY, overlay);
        setScene(scene);
        Tessel.apply(scene);

        installResizing();
        windowMaximized.addListener((o, a, b) -> applyMaximizedState(b));
        focusedProperty().addListener((o, a, b) -> shell.pseudoClassStateChanged(ACTIVE, b));
        shell.pseudoClassStateChanged(ACTIVE, true);
        getIcons().addListener((javafx.collections.ListChangeListener<javafx.scene.image.Image>) c -> updateIcon());
        resizableProperty().addListener((o, a, b) -> {
            maximizeButton.setVisible(b && !dialog);
            maximizeButton.setManaged(b && !dialog);
        });
        setMinWidth(2 * SHADOW + 320);
        setMinHeight(2 * SHADOW + TITLE_HEIGHT + 80);
    }

    // ------------------------------------------------------------------ title bar

    private void buildTitleBar(boolean dialog) {
        titleBar.getStyleClass().add("tsl-titlebar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setMinHeight(TITLE_HEIGHT);
        titleBar.setPrefHeight(TITLE_HEIGHT);
        titleBar.setMaxHeight(TITLE_HEIGHT);

        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);
        iconView.fitWidthProperty().bind(iconSize);
        iconView.fitHeightProperty().bind(iconSize);
        iconView.getStyleClass().add("tsl-titlebar-icon");
        iconView.managedProperty().bind(iconView.visibleProperty());
        iconView.setVisible(false);

        Label titleLabel = new Label();
        titleLabel.textProperty().bind(titleProperty());
        titleLabel.getStyleClass().addAll("tsl-window-title", "tsl-semibold");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setMouseTransparent(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        titleComponents.setAlignment(Pos.CENTER_RIGHT);
        titleComponents.getStyleClass().add("tsl-titlebar-components");

        setupCaption(minimizeButton, Symbol.CHROME_MINIMIZE, false);
        setupCaption(maximizeButton, Symbol.CHROME_MAXIMIZE, false);
        setupCaption(closeButton, Symbol.CHROME_CLOSE, true);
        minimizeButton.setOnAction(e -> setIconified(true));
        maximizeButton.setOnAction(e -> toggleMaximize());
        closeButton.setOnAction(e -> fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)));
        if (dialog) {
            minimizeButton.setVisible(false);
            minimizeButton.setManaged(false);
            maximizeButton.setVisible(false);
            maximizeButton.setManaged(false);
        }

        HBox caption = new HBox(minimizeButton, maximizeButton, closeButton);
        caption.getStyleClass().add("tsl-caption");
        titleBar.getChildren().addAll(iconView, titleLabel, titleComponents, caption);

        titleBar.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && edge == 0) {
                dragOffsetX = e.getScreenX() - getX();
                dragOffsetY = e.getScreenY() - getY();
                titleDragging = true;
            }
        });
        titleBar.setOnMouseDragged(e -> {
            if (!titleDragging || e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (windowMaximized.get() && isResizable()) {
                // dragging a maximized window restores it under the pointer
                double ratio = e.getScreenX() <= getX() ? 0.5 : (e.getScreenX() - getX()) / getWidth();
                restore();
                dragOffsetX = getWidth() * ratio;
                dragOffsetY = Math.min(e.getY() + SHADOW, TITLE_HEIGHT);
            }
            setX(e.getScreenX() - dragOffsetX);
            setY(e.getScreenY() - dragOffsetY);
        });
        titleBar.setOnMouseReleased(e -> titleDragging = false);
        titleBar.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && isResizable()
                    && maximizeButton.isManaged()) {
                toggleMaximize();
            }
        });
    }

    private void setupCaption(Button b, Symbol symbol, boolean close) {
        b.getStyleClass().add("tsl-caption-button");
        if (close) {
            b.getStyleClass().add("tsl-caption-close");
        }
        b.setGraphic(new Icon(symbol, 16));
        b.setFocusTraversable(false);
    }

    private void updateIcon() {
        ObservableList<javafx.scene.image.Image> icons = getIcons();
        iconView.setImage(icons.isEmpty() ? null : icons.get(0));
        iconView.setVisible(!icons.isEmpty());
    }

    /** Adds a component to the title bar, left of the minimize / maximize / close buttons. */
    public void addTitleBarComponent(Node node) {
        titleComponents.getChildren().add(node);
    }

    /** The components in the title bar (left of the window buttons). */
    public ObservableList<Node> getTitleBarComponents() {
        return titleComponents.getChildren();
    }

    /** The size of the title bar icon (default 16). */
    public final double getIconSize() {
        return iconSize.get();
    }

    public final void setIconSize(double size) {
        iconSize.set(size);
    }

    public final DoubleProperty iconSizeProperty() {
        return iconSize;
    }

    // ------------------------------------------------------------------ content

    /** Sets what the window shows below the title bar. */
    public void setContent(Node content) {
        contentHost.getChildren().setAll(content == null ? new Node[0] : new Node[]{content});
    }

    public Node getContent() {
        return contentHost.getChildren().isEmpty() ? null : contentHost.getChildren().get(0);
    }

    /** Sets the size of the area below the title bar (the window is larger by the frame and title bar). */
    public void setContentSize(double width, double height) {
        setWidth(width + 2 * SHADOW + 2);
        setHeight(height + 2 * SHADOW + 2 + TITLE_HEIGHT);
    }

    /** Sets the minimum size of the area below the title bar. */
    public void setContentMinSize(double width, double height) {
        setMinWidth(width + 2 * SHADOW + 2);
        setMinHeight(height + 2 * SHADOW + 2 + TITLE_HEIGHT);
    }

    /** The layer above the content where in-window dialogs, snackbars and teaching tips are drawn. */
    public StackPane getOverlayLayer() {
        return overlay;
    }

    // ------------------------------------------------------------------ maximize

    /** True while the window is maximized by {@link #toggleMaximize()}. */
    public final boolean isWindowMaximized() {
        return windowMaximized.get();
    }

    public final ReadOnlyBooleanProperty windowMaximizedProperty() {
        return windowMaximized;
    }

    public void toggleMaximize() {
        if (windowMaximized.get()) {
            restore();
        } else {
            maximize();
        }
    }

    public void maximize() {
        if (windowMaximized.get() || !isResizable()) {
            return;
        }
        restoreBounds = new Rectangle2D(getX(), getY(), getWidth(), getHeight());
        Rectangle2D area = screenArea();
        windowMaximized.set(true);
        setX(area.getMinX());
        setY(area.getMinY());
        setWidth(area.getWidth());
        setHeight(area.getHeight());
    }

    public void restore() {
        if (!windowMaximized.get()) {
            return;
        }
        windowMaximized.set(false);
        if (restoreBounds != null) {
            setX(restoreBounds.getMinX());
            setY(restoreBounds.getMinY());
            setWidth(restoreBounds.getWidth());
            setHeight(restoreBounds.getHeight());
        }
    }

    private Rectangle2D screenArea() {
        double cx = getX() + getWidth() / 2;
        double cy = getY() + getHeight() / 2;
        for (Screen s : Screen.getScreensForRectangle(cx, cy, 1, 1)) {
            return s.getVisualBounds();
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private void applyMaximizedState(boolean max) {
        shell.pseudoClassStateChanged(MAXIMIZED, max);
        clip.setArcWidth(max ? 0 : 14);
        clip.setArcHeight(max ? 0 : 14);
        maximizeButton.setGraphic(new Icon(max ? Symbol.CHROME_RESTORE : Symbol.CHROME_MAXIMIZE, 16));
    }

    // ------------------------------------------------------------------ resizing

    private void installResizing() {
        shell.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (edge == 0) {
                shell.setCursor(cursorFor(edgeAt(e.getSceneX(), e.getSceneY())));
            }
        });
        shell.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (edge == 0) {
                shell.setCursor(null);
            }
        });
        shell.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            int at = edgeAt(e.getSceneX(), e.getSceneY());
            if (e.getButton() == MouseButton.PRIMARY && at != 0) {
                edge = at;
                startX = getX();
                startY = getY();
                startW = getWidth();
                startH = getHeight();
                startMouseX = e.getScreenX();
                startMouseY = e.getScreenY();
                e.consume();
            }
        });
        shell.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (edge == 0) {
                return;
            }
            double dx = e.getScreenX() - startMouseX;
            double dy = e.getScreenY() - startMouseY;
            double x = startX;
            double y = startY;
            double w = startW;
            double h = startH;
            if ((edge & RIGHT) != 0) {
                w = Math.max(getMinWidth(), startW + dx);
            }
            if ((edge & BOTTOM) != 0) {
                h = Math.max(getMinHeight(), startH + dy);
            }
            if ((edge & LEFT) != 0) {
                w = Math.max(getMinWidth(), startW - dx);
                x = startX + startW - w;
            }
            if ((edge & TOP) != 0) {
                h = Math.max(getMinHeight(), startH - dy);
                y = startY + startH - h;
            }
            setX(x);
            setY(y);
            setWidth(w);
            setHeight(h);
            e.consume();
        });
        shell.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (edge != 0) {
                edge = 0;
                e.consume();
            }
        });
    }

    private int edgeAt(double x, double y) {
        if (windowMaximized.get() || !isResizable()) {
            return 0;
        }
        double w = scene.getWidth();
        double h = scene.getHeight();
        double l = SHADOW;
        double r = w - SHADOW;
        double t = SHADOW;
        double b = h - SHADOW;
        if (x < l - 3 || x > r + 3 || y < t - 3 || y > b + 3) {
            return 0;
        }
        int at = 0;
        if (x <= l + RESIZE_ZONE) {
            at |= LEFT;
        } else if (x >= r - RESIZE_ZONE) {
            at |= RIGHT;
        }
        if (y <= t + RESIZE_ZONE) {
            at |= TOP;
        } else if (y >= b - RESIZE_ZONE) {
            at |= BOTTOM;
        }
        return at;
    }

    private static Cursor cursorFor(int edge) {
        switch (edge) {
            case LEFT:
            case RIGHT:
                return Cursor.H_RESIZE;
            case TOP:
            case BOTTOM:
                return Cursor.V_RESIZE;
            case LEFT | TOP:
            case RIGHT | BOTTOM:
                return Cursor.NW_RESIZE;
            case RIGHT | TOP:
            case LEFT | BOTTOM:
                return Cursor.NE_RESIZE;
            default:
                return null;
        }
    }

    /** The root of the window's scene. */
    protected final Region getShell() {
        return shell;
    }
}
