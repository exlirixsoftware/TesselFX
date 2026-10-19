package net.tessel.fx.components;

import java.util.function.Consumer;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * A popover with a tail that points at a control, to explain a feature.
 *
 * <pre>{@code
 * TeachingTip tip = new TeachingTip("Save your work", "Press Ctrl+S at any time.");
 * tip.setActionButton("Got it", () -> markTipSeen());
 * tip.show(saveButton);
 * }</pre>
 *
 * The tip is drawn inside the window, follows its target when the window is resized or scrolled, and closes on Esc or
 * on a click elsewhere (see {@link #setLightDismiss(boolean)}).
 */
public class TeachingTip {

    /** Which side of the target the tip appears on. */
    public enum Placement { AUTO, TOP, BOTTOM, LEFT, RIGHT }

    /** Why a tip was closed. */
    public enum CloseReason { CLOSE_BUTTON, ACTION_BUTTON, LIGHT_DISMISS, PROGRAMMATIC, TARGET_HIDDEN }

    private static final double TAIL = 12;
    private static final double MARGIN = 8;
    private static final double GAP = 6;

    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty subtitle = new SimpleStringProperty("");
    private final ObjectProperty<Symbol> symbol = new SimpleObjectProperty<>();
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>();
    private final ObjectProperty<Placement> placement = new SimpleObjectProperty<>(Placement.AUTO);
    private final BooleanProperty lightDismiss = new SimpleBooleanProperty(true);
    private final BooleanProperty tailVisible = new SimpleBooleanProperty(true);
    private final StringProperty actionText = new SimpleStringProperty("");
    private final StringProperty closeText = new SimpleStringProperty("");
    private Runnable action;
    private Consumer<CloseReason> onClosed;

    private Presenter presenter;

    public TeachingTip() {
        this("", "");
    }

    public TeachingTip(String title, String subtitle) {
        setTitle(title);
        setSubtitle(subtitle);
    }

    /** Sets the accent button of the tip; the tip closes when it is pressed. */
    public void setActionButton(String text, Runnable action) {
        this.actionText.set(text == null ? "" : text);
        this.action = action;
    }

    public boolean isShowing() {
        return presenter != null;
    }

    /** Shows the tip next to a node; a tip that is already showing moves to the new target. */
    public void show(Node target) {
        if (target.getScene() == null) {
            throw new IllegalArgumentException("The target is not part of a scene");
        }
        if (presenter != null) {
            close(CloseReason.PROGRAMMATIC);
        }
        presenter = new Presenter(target);
        presenter.open();
    }

    public void close() {
        close(CloseReason.PROGRAMMATIC);
    }

    private void close(CloseReason reason) {
        Presenter p = presenter;
        if (p == null) {
            return;
        }
        presenter = null;
        p.dispose();
        if (onClosed != null) {
            onClosed.accept(reason);
        }
    }

    private final class Presenter {
        private final Node target;
        private final Scene scene;
        private final StackPane layer;
        private final Pane2 pane = new Pane2();
        private final VBox body = new VBox(8);
        private final Region tail = new Region();
        private final InvalidationListener follow = o -> reposition();
        private final EventHandler<MouseEvent> pressFilter = this::onPress;
        private final EventHandler<KeyEvent> keyFilter = this::onKey;
        private Placement resolved = Placement.BOTTOM;

        Presenter(Node target) {
            this.target = target;
            this.scene = target.getScene();
            this.layer = Overlays.layer(scene);
        }

        void open() {
            body.getStyleClass().add("tsl-tip-body");
            body.setMaxWidth(320);
            body.setMinWidth(200);
            body.setPrefWidth(300);
            tail.getStyleClass().add("tsl-tip-tail");
            tail.setMinSize(TAIL, TAIL);
            tail.setPrefSize(TAIL, TAIL);
            tail.setMaxSize(TAIL, TAIL);
            tail.setRotate(45);
            tail.setMouseTransparent(true);
            tail.visibleProperty().bind(tailVisible);

            Label titleLabel = new Label();
            titleLabel.textProperty().bind(title);
            titleLabel.getStyleClass().addAll("tsl-tip-title", "tsl-semibold");
            titleLabel.setWrapText(true);
            titleLabel.setMinHeight(Region.USE_PREF_SIZE);
            Label subtitleLabel = new Label();
            subtitleLabel.textProperty().bind(subtitle);
            subtitleLabel.getStyleClass().add("tsl-tip-subtitle");
            subtitleLabel.setWrapText(true);
            subtitleLabel.setMinHeight(Region.USE_PREF_SIZE);
            titleLabel.managedProperty().bind(titleLabel.visibleProperty());
            titleLabel.visibleProperty().bind(title.isNotEmpty());
            subtitleLabel.managedProperty().bind(subtitleLabel.visibleProperty());
            subtitleLabel.visibleProperty().bind(subtitle.isNotEmpty());

            VBox text = new VBox(4, titleLabel, subtitleLabel);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox head = new HBox(10);
            head.setAlignment(Pos.TOP_LEFT);
            if (symbol.get() != null) {
                Icon icon = new Icon(symbol.get(), 20);
                icon.getStyleClass().add("tsl-tip-icon");
                head.getChildren().add(icon);
            }
            Button x = Tessel.style(new Button(), ButtonStyle.ICON);
            x.setGraphic(new Icon(Symbol.CHROME_CLOSE, 12));
            x.setMinSize(24, 24);
            x.setPrefSize(24, 24);
            x.setMaxSize(24, 24);
            x.setOnAction(e -> close(CloseReason.CLOSE_BUTTON));
            head.getChildren().addAll(text, x);
            body.getChildren().add(head);

            if (content.get() != null) {
                body.getChildren().add(content.get());
            }
            boolean hasAction = !actionText.get().isEmpty();
            boolean hasClose = !closeText.get().isEmpty();
            if (hasAction || hasClose) {
                HBox buttons = new HBox(8);
                buttons.setAlignment(Pos.CENTER_RIGHT);
                if (hasAction) {
                    Button a = Tessel.style(new Button(actionText.get()), ButtonStyle.ACCENT);
                    a.setOnAction(e -> {
                        Runnable r = action;
                        close(CloseReason.ACTION_BUTTON);
                        if (r != null) {
                            r.run();
                        }
                    });
                    buttons.getChildren().add(a);
                }
                if (hasClose) {
                    Button c = new Button(closeText.get());
                    c.setOnAction(e -> close(CloseReason.CLOSE_BUTTON));
                    buttons.getChildren().add(c);
                }
                body.getChildren().add(buttons);
            }

            pane.getStyleClass().add("tsl-teaching-tip");
            pane.getChildren().addAll(tail, body);
            pane.setManaged(false);
            pane.setPickOnBounds(false);
            pane.setOpacity(0);
            layer.getChildren().add(pane);
            StackPane.setAlignment(pane, Pos.TOP_LEFT);

            target.localToSceneTransformProperty().addListener(follow);
            target.boundsInLocalProperty().addListener(follow);
            target.sceneProperty().addListener(follow);
            target.visibleProperty().addListener(follow);
            layer.layoutBoundsProperty().addListener(follow);
            scene.addEventFilter(MouseEvent.MOUSE_PRESSED, pressFilter);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            javafx.application.Platform.runLater(() -> {
                if (presenter != this) {
                    return;
                }
                pane.applyCss(); // the sizes below depend on the padding from the style sheet
                reposition();
                pane.setOpacity(1);
            });
        }

        void dispose() {
            target.localToSceneTransformProperty().removeListener(follow);
            target.boundsInLocalProperty().removeListener(follow);
            target.sceneProperty().removeListener(follow);
            target.visibleProperty().removeListener(follow);
            layer.layoutBoundsProperty().removeListener(follow);
            scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, pressFilter);
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            tail.visibleProperty().unbind();
            layer.getChildren().remove(pane);
        }

        private void onPress(MouseEvent e) {
            Node hit = e.getPickResult().getIntersectedNode();
            if (!lightDismiss.get() || hit == null) {
                return;
            }
            for (Node n = hit; n != null; n = n.getParent()) {
                if (n == pane || n == body) {
                    return; // inside the tip
                }
            }
            close(CloseReason.LIGHT_DISMISS);
        }

        private void onKey(KeyEvent e) {
            if (lightDismiss.get() && e.getCode() == KeyCode.ESCAPE) {
                close(CloseReason.LIGHT_DISMISS);
            }
        }

        private void reposition() {
            if (presenter != this) {
                return;
            }
            if (target.getScene() != scene || !target.isVisible() || !isShowingInTree(target)) {
                close(CloseReason.TARGET_HIDDEN);
                return;
            }
            Bounds t = layer.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
            if (t == null) {
                return;
            }
            double w = body.prefWidth(-1);
            w = Math.min(Math.max(w, body.getMinWidth()), body.getMaxWidth());
            double h = body.prefHeight(w);
            double lw = layer.getWidth();
            double lh = layer.getHeight();
            Placement p = placement.get() == Placement.AUTO ? choose(t, w, h, lw, lh) : placement.get();
            resolved = p;
            double tail = tailVisible.get() ? TAIL / 2 : 0;
            double x;
            double y;
            switch (p) {
                case TOP:
                    y = t.getMinY() - GAP - tail - h;
                    x = t.getCenterX() - w / 2;
                    break;
                case LEFT:
                    x = t.getMinX() - GAP - tail - w;
                    y = t.getCenterY() - h / 2;
                    break;
                case RIGHT:
                    x = t.getMaxX() + GAP + tail;
                    y = t.getCenterY() - h / 2;
                    break;
                default:
                    y = t.getMaxY() + GAP + tail;
                    x = t.getCenterX() - w / 2;
                    break;
            }
            x = Math.max(MARGIN, Math.min(x, lw - w - MARGIN));
            y = Math.max(MARGIN, Math.min(y, lh - h - MARGIN));
            body.resizeRelocate(x, y, w, h);

            // the tail sits on the side facing the target, centered on it but kept clear of the corners
            double cx;
            double cy;
            double edge = TAIL / 2;
            double corner = 14 + edge;
            switch (p) {
                case TOP:
                    cx = clamp(t.getCenterX(), x + corner, x + w - corner);
                    cy = y + h;
                    break;
                case LEFT:
                    cx = x + w;
                    cy = clamp(t.getCenterY(), y + corner, y + h - corner);
                    break;
                case RIGHT:
                    cx = x;
                    cy = clamp(t.getCenterY(), y + corner, y + h - corner);
                    break;
                default:
                    cx = clamp(t.getCenterX(), x + corner, x + w - corner);
                    cy = y;
                    break;
            }
            this.tail.resizeRelocate(cx - TAIL / 2, cy - TAIL / 2, TAIL, TAIL);
        }

        private Placement choose(Bounds t, double w, double h, double lw, double lh) {
            double below = lh - t.getMaxY();
            double above = t.getMinY();
            double right = lw - t.getMaxX();
            double left = t.getMinX();
            double need = h + GAP + TAIL / 2 + MARGIN;
            if (below >= need) {
                return Placement.BOTTOM;
            }
            if (above >= need) {
                return Placement.TOP;
            }
            double needW = w + GAP + TAIL / 2 + MARGIN;
            if (right >= needW) {
                return Placement.RIGHT;
            }
            if (left >= needW) {
                return Placement.LEFT;
            }
            double best = Math.max(Math.max(below, above), Math.max(right, left));
            return best == below ? Placement.BOTTOM : best == above ? Placement.TOP : best == right ? Placement.RIGHT : Placement.LEFT;
        }

        private double clamp(double v, double min, double max) {
            return max < min ? (min + max) / 2 : Math.max(min, Math.min(max, v));
        }

        private boolean isShowingInTree(Node n) {
            for (Node p = n; p != null; p = p.getParent()) {
                if (!p.isVisible()) {
                    return false;
                }
            }
            return n.getScene() != null;
        }
    }

    /** A pane that lays out nothing itself: the presenter places the children. */
    private static final class Pane2 extends javafx.scene.layout.Pane {
        @Override
        protected void layoutChildren() {
            // children are positioned by the presenter
        }
    }

    // ------------------------------------------------------------------ properties

    public final String getTitle() {
        return title.get();
    }

    public final void setTitle(String title) {
        this.title.set(title == null ? "" : title);
    }

    public final StringProperty titleProperty() {
        return title;
    }

    public final String getSubtitle() {
        return subtitle.get();
    }

    public final void setSubtitle(String subtitle) {
        this.subtitle.set(subtitle == null ? "" : subtitle);
    }

    public final StringProperty subtitleProperty() {
        return subtitle;
    }

    /** An icon shown before the title; set it before showing the tip. */
    public final Symbol getSymbol() {
        return symbol.get();
    }

    public final void setSymbol(Symbol symbol) {
        this.symbol.set(symbol);
    }

    public final ObjectProperty<Symbol> symbolProperty() {
        return symbol;
    }

    /** Any component shown between the text and the buttons; set it before showing the tip. */
    public final Node getContent() {
        return content.get();
    }

    public final void setContent(Node content) {
        this.content.set(content);
    }

    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    public final Placement getPlacement() {
        return placement.get();
    }

    public final void setPlacement(Placement placement) {
        this.placement.set(placement == null ? Placement.AUTO : placement);
    }

    public final ObjectProperty<Placement> placementProperty() {
        return placement;
    }

    /** True (the default) to close the tip on Esc and on a click outside of it. */
    public final boolean isLightDismiss() {
        return lightDismiss.get();
    }

    public final void setLightDismiss(boolean lightDismiss) {
        this.lightDismiss.set(lightDismiss);
    }

    public final BooleanProperty lightDismissProperty() {
        return lightDismiss;
    }

    /** False to draw a plain flyout without the tail. */
    public final boolean isTailVisible() {
        return tailVisible.get();
    }

    public final void setTailVisible(boolean tailVisible) {
        this.tailVisible.set(tailVisible);
    }

    public final BooleanProperty tailVisibleProperty() {
        return tailVisible;
    }

    /** The text of a plain button next to the action button; empty for none. */
    public final String getCloseButtonText() {
        return closeText.get();
    }

    public final void setCloseButtonText(String text) {
        closeText.set(text == null ? "" : text);
    }

    public final StringProperty closeButtonTextProperty() {
        return closeText;
    }

    /** Called after the tip closed, with the reason. */
    public final void setOnClosed(Consumer<CloseReason> onClosed) {
        this.onClosed = onClosed;
    }
}
