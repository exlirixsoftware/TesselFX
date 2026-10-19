package net.tessel.fx.components;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;

/**
 * A modal dialog drawn over the window's own content (with a dimmed backdrop), for confirmations and short forms.
 *
 * <pre>{@code
 * ContentDialog dialog = new ContentDialog("Delete file?", "This can't be undone.");
 * dialog.setPrimaryButtonText("Delete");
 * dialog.setCloseButtonText("Cancel");
 * dialog.setPrimaryDestructive(true);
 * if (dialog.showAndWait(window) == ContentDialog.Result.PRIMARY) { ... }
 * }</pre>
 *
 * Use {@link #show(Window)} for the asynchronous variant that returns a {@link CompletableFuture}. Esc closes the
 * dialog with {@link Result#NONE}; Enter presses the default button.
 */
public class ContentDialog {

    /** How the dialog was closed. */
    public enum Result {
        /** Closed with Esc or the close button. */
        NONE,
        PRIMARY,
        SECONDARY
    }

    private final StringProperty title = new SimpleStringProperty("");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>();
    private final StringProperty primaryButtonText = new SimpleStringProperty("");
    private final StringProperty secondaryButtonText = new SimpleStringProperty("");
    private final StringProperty closeButtonText = new SimpleStringProperty("Close");
    private final BooleanProperty primaryDestructive = new SimpleBooleanProperty(false);
    private final BooleanProperty primaryButtonDisabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<Result> defaultResult = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Predicate<Result>> onClosing = new SimpleObjectProperty<>();

    private boolean open;

    public ContentDialog() {
        this("", (Node) null);
    }

    public ContentDialog(String title, String message) {
        this(title, message == null ? null : messageNode(message));
    }

    public ContentDialog(String title, Node content) {
        setTitle(title);
        setContent(content);
    }

    private static Node messageNode(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.getStyleClass().add("tsl-dialog-message");
        return label;
    }

    // ------------------------------------------------------------------ showing

    /** Shows the dialog over the window and returns a future that completes when it is closed. */
    public CompletableFuture<Result> show(Window owner) {
        return show(Overlays.sceneOf(owner));
    }

    public CompletableFuture<Result> show(Node owner) {
        return show(Overlays.sceneOf(owner));
    }

    public CompletableFuture<Result> show(Scene scene) {
        if (open) {
            throw new IllegalStateException("The dialog is already shown");
        }
        open = true;
        CompletableFuture<Result> future = new CompletableFuture<>();
        new Presenter(scene, future).present();
        return future;
    }

    /**
     * Shows the dialog and waits for the user's answer. The rest of the window stays responsive to repainting, but
     * input goes to the dialog. Must be called on the JavaFX application thread.
     */
    public Result showAndWait(Window owner) {
        return await(show(owner));
    }

    public Result showAndWait(Node owner) {
        return await(show(owner));
    }

    public Result showAndWait(Scene scene) {
        return await(show(scene));
    }

    private static Result await(CompletableFuture<Result> future) {
        if (!future.isDone()) {
            Object key = new Object();
            future.thenAccept(r -> Platform.runLater(() -> Platform.exitNestedEventLoop(key, null)));
            Platform.enterNestedEventLoop(key);
        }
        return future.getNow(Result.NONE);
    }

    private final class Presenter {
        private final Scene scene;
        private final CompletableFuture<Result> future;
        private final StackPane layer;
        private final StackPane scrim = new StackPane();
        private final VBox panel = new VBox();
        private final List<Button> buttons = new ArrayList<>();
        private Button primary;
        private Button secondary;
        private Button close;
        private Node previousFocus;
        private boolean closing;
        private final EventHandler<KeyEvent> keys = this::onKey;

        Presenter(Scene scene, CompletableFuture<Result> future) {
            this.scene = scene;
            this.future = future;
            this.layer = Overlays.layer(scene);
        }

        void present() {
            previousFocus = scene.getFocusOwner();

            scrim.getStyleClass().add("tsl-dialog-scrim");
            scrim.setAlignment(Pos.CENTER);
            panel.getStyleClass().add("tsl-dialog-panel");
            panel.setMinWidth(320);
            panel.setPrefWidth(420);
            panel.setMaxWidth(548);
            panel.setMaxHeight(Region.USE_PREF_SIZE);

            Label titleLabel = new Label();
            titleLabel.textProperty().bind(title);
            titleLabel.getStyleClass().addAll("tsl-dialog-title", "tsl-semibold");
            titleLabel.setWrapText(true);
            titleLabel.setMinHeight(Region.USE_PREF_SIZE);
            titleLabel.managedProperty().bind(titleLabel.visibleProperty());
            titleLabel.visibleProperty().bind(title.isNotEmpty());

            VBox body = new VBox(12, titleLabel);
            body.getStyleClass().add("tsl-dialog-body");
            Node custom = getContent();
            if (custom != null) {
                body.getChildren().add(custom);
            }
            VBox.setVgrow(body, Priority.ALWAYS);

            HBox footer = new HBox(8);
            footer.getStyleClass().add("tsl-dialog-footer");
            footer.setAlignment(Pos.CENTER_RIGHT);
            if (!getPrimaryButtonText().isEmpty()) {
                primary = button(getPrimaryButtonText(), isPrimaryDestructive() ? ButtonStyle.DANGER : ButtonStyle.ACCENT,
                        Result.PRIMARY);
                primary.disableProperty().bind(primaryButtonDisabled);
                footer.getChildren().add(primary);
            }
            if (!getSecondaryButtonText().isEmpty()) {
                secondary = button(getSecondaryButtonText(), ButtonStyle.STANDARD, Result.SECONDARY);
                footer.getChildren().add(secondary);
            }
            if (!getCloseButtonText().isEmpty() || footer.getChildren().isEmpty()) {
                close = button(getCloseButtonText().isEmpty() ? "Close" : getCloseButtonText(),
                        ButtonStyle.STANDARD, Result.NONE);
                footer.getChildren().add(close);
            }
            panel.getChildren().addAll(body, footer);
            scrim.getChildren().add(panel);
            layer.getChildren().add(scrim);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, keys);

            Button initial = defaultButton();
            Platform.runLater(() -> {
                if (initial != null) {
                    initial.requestFocus();
                }
            });

            scrim.setOpacity(0);
            panel.setScaleX(0.96);
            panel.setScaleY(0.96);
            FadeTransition fade = new FadeTransition(Duration.millis(200), scrim);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(220), panel);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(Collapsible.EASE);
            new ParallelTransition(fade, scale).play();
        }

        private Button button(String text, ButtonStyle style, Result result) {
            Button b = new Button(text);
            b.setMnemonicParsing(false);
            Tessel.style(b, style);
            b.setOnAction(e -> finish(result));
            buttons.add(b);
            return b;
        }

        private Button defaultButton() {
            Result d = getDefaultResult();
            if (d == Result.PRIMARY && primary != null) {
                return primary;
            }
            if (d == Result.SECONDARY && secondary != null) {
                return secondary;
            }
            if (d == Result.NONE && close != null) {
                return close;
            }
            if (primary != null) {
                return primary;
            }
            return close != null ? close : secondary;
        }

        private void onKey(KeyEvent e) {
            if (closing || e.getEventType() != KeyEvent.KEY_PRESSED) {
                return;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                finish(Result.NONE);
            } else if (e.getCode() == KeyCode.ENTER) {
                Node focus = scene.getFocusOwner();
                Button target = focus instanceof Button && buttons.contains(focus) ? (Button) focus : null;
                if (target == null && !(focus instanceof javafx.scene.control.TextArea)) {
                    target = defaultButton();
                }
                if (target != null && !target.isDisabled()) {
                    e.consume();
                    target.fire();
                }
            } else if (e.getCode() == KeyCode.TAB) {
                e.consume();
                cycleFocus(e.isShiftDown());
            }
        }

        /** Keeps the focus inside the dialog. */
        private void cycleFocus(boolean backwards) {
            List<Node> focusable = new ArrayList<>();
            for (Node n : panel.lookupAll("*")) {
                if (n.isFocusTraversable() && !n.isDisabled() && n.isVisible() && n.getScene() != null) {
                    focusable.add(n);
                }
            }
            if (focusable.isEmpty()) {
                return;
            }
            Node focus = scene.getFocusOwner();
            int at = -1;
            for (int i = 0; i < focusable.size(); i++) {
                Node n = focusable.get(i);
                if (n == focus || (focus != null && isAncestor(n, focus))) {
                    at = i;
                    break;
                }
            }
            int next = at < 0 ? 0 : (at + (backwards ? focusable.size() - 1 : 1)) % focusable.size();
            focusable.get(next).requestFocus();
        }

        private boolean isAncestor(Node ancestor, Node node) {
            for (Node n = node; n != null; n = n.getParent()) {
                if (n == ancestor) {
                    return true;
                }
            }
            return false;
        }

        private void finish(Result result) {
            if (closing) {
                return;
            }
            Predicate<Result> veto = getOnClosing();
            if (veto != null && !veto.test(result)) {
                return;
            }
            closing = true;
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keys);
            FadeTransition fade = new FadeTransition(Duration.millis(140), scrim);
            fade.setToValue(0);
            fade.setOnFinished(e -> {
                layer.getChildren().remove(scrim);
                open = false;
                if (previousFocus != null && previousFocus.getScene() == scene) {
                    previousFocus.requestFocus();
                }
                future.complete(result);
            });
            fade.play();
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

    /** The content below the title. A plain message is wrapped in a label by the constructor. */
    public final Node getContent() {
        return content.get();
    }

    public final void setContent(Node content) {
        this.content.set(content);
    }

    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    public final String getPrimaryButtonText() {
        return primaryButtonText.get();
    }

    public final void setPrimaryButtonText(String text) {
        primaryButtonText.set(text == null ? "" : text);
    }

    public final StringProperty primaryButtonTextProperty() {
        return primaryButtonText;
    }

    public final String getSecondaryButtonText() {
        return secondaryButtonText.get();
    }

    public final void setSecondaryButtonText(String text) {
        secondaryButtonText.set(text == null ? "" : text);
    }

    public final StringProperty secondaryButtonTextProperty() {
        return secondaryButtonText;
    }

    /** The text of the close button (default "Close"); an empty text leaves the dialog without one. */
    public final String getCloseButtonText() {
        return closeButtonText.get();
    }

    public final void setCloseButtonText(String text) {
        closeButtonText.set(text == null ? "" : text);
    }

    public final StringProperty closeButtonTextProperty() {
        return closeButtonText;
    }

    /** True to draw the primary button in the danger color (delete, discard...). */
    public final boolean isPrimaryDestructive() {
        return primaryDestructive.get();
    }

    public final void setPrimaryDestructive(boolean destructive) {
        primaryDestructive.set(destructive);
    }

    public final BooleanProperty primaryDestructiveProperty() {
        return primaryDestructive;
    }

    /** Disables the primary button, for example while a form is incomplete. Can change while the dialog is open. */
    public final boolean isPrimaryButtonDisabled() {
        return primaryButtonDisabled.get();
    }

    public final void setPrimaryButtonDisabled(boolean disabled) {
        primaryButtonDisabled.set(disabled);
    }

    public final BooleanProperty primaryButtonDisabledProperty() {
        return primaryButtonDisabled;
    }

    /** The button that has the focus first and that Enter presses; the primary button by default. */
    public final Result getDefaultResult() {
        return defaultResult.get();
    }

    public final void setDefaultResult(Result result) {
        defaultResult.set(result);
    }

    public final ObjectProperty<Result> defaultResultProperty() {
        return defaultResult;
    }

    /** A test run before the dialog closes; return false to keep it open (for validation). */
    public final Predicate<Result> getOnClosing() {
        return onClosing.get();
    }

    public final void setOnClosing(Predicate<Result> onClosing) {
        this.onClosing.set(onClosing);
    }

    public final ObjectProperty<Predicate<Result>> onClosingProperty() {
        return onClosing;
    }
}
