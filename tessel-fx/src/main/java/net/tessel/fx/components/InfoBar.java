package net.tessel.fx.components;

import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Severity;
import net.tessel.fx.Tessel;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * An inline message with a severity: informational, success, warning or error.
 *
 * <pre>{@code
 * InfoBar bar = new InfoBar(Severity.WARNING, "Heads up", "Your subscription expires soon.");
 * bar.setClosable(true);
 * }</pre>
 */
public class InfoBar extends HBox {

    private final ObjectProperty<Severity> severity = new SimpleObjectProperty<>(Severity.INFORMATIONAL);
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty message = new SimpleStringProperty("");
    private final BooleanProperty closable = new SimpleBooleanProperty(false);
    private final BooleanProperty elevated = new SimpleBooleanProperty(false);
    private final ObjectProperty<Node> action = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onClosed = new SimpleObjectProperty<>();

    private final Icon icon = new Icon(Severity.INFORMATIONAL.iconKey(), 20);
    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();
    private final StackPane actionHost = new StackPane();
    private final Button closeButton = new Button();

    public InfoBar(Severity severity, String title, String message) {
        getStyleClass().add("tsl-info-bar");
        setAlignment(Pos.TOP_LEFT);

        icon.getStyleClass().add("tsl-info-icon");
        StackPane iconHost = new StackPane(icon);
        iconHost.setAlignment(Pos.TOP_LEFT);
        iconHost.getStyleClass().add("tsl-info-icon-host");

        titleLabel.getStyleClass().addAll("tsl-info-title", "tsl-semibold");
        messageLabel.getStyleClass().add("tsl-info-message");
        messageLabel.setWrapText(true);
        messageLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        VBox body = new VBox(2, titleLabel, messageLabel);
        body.getStyleClass().add("tsl-info-body");
        HBox.setHgrow(body, Priority.ALWAYS);

        actionHost.setAlignment(Pos.CENTER_RIGHT);
        actionHost.managedProperty().bind(actionHost.visibleProperty());
        actionHost.setVisible(false);

        closeButton.setGraphic(new Icon(Symbol.CHROME_CLOSE, 14));
        closeButton.getStyleClass().add("tsl-info-close");
        Tessel.style(closeButton, ButtonStyle.ICON);
        closeButton.setFocusTraversable(true);
        closeButton.setOnAction(e -> dismiss());
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.visibleProperty().bind(closable);

        getChildren().addAll(iconHost, body, actionHost, closeButton);

        titleLabel.textProperty().bind(this.title);
        titleLabel.visibleProperty().bind(this.title.isNotEmpty());
        titleLabel.managedProperty().bind(titleLabel.visibleProperty());
        messageLabel.textProperty().bind(this.message);
        messageLabel.visibleProperty().bind(this.message.isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        this.severity.addListener((o, a, b) -> applySeverity(a, b));
        this.elevated.addListener((o, a, b) -> {
            if (b) {
                getStyleClass().add("tsl-elevated");
            } else {
                getStyleClass().remove("tsl-elevated");
            }
        });
        this.action.addListener((o, a, b) -> {
            actionHost.getChildren().clear();
            actionHost.setVisible(b != null);
            if (b != null) {
                actionHost.getChildren().add(b);
            }
        });

        setSeverity(severity == null ? Severity.INFORMATIONAL : severity);
        applySeverity(null, getSeverity());
        setTitle(title);
        setMessage(message);
    }

    public InfoBar(Severity severity, String message) {
        this(severity, null, message);
    }

    private void applySeverity(Severity old, Severity now) {
        if (old != null) {
            getStyleClass().remove(old.styleClass());
        }
        if (!getStyleClass().contains(now.styleClass())) {
            getStyleClass().add(now.styleClass());
        }
        icon.setKey(now.iconKey());
    }

    /** Fades the bar out, removes it from its parent and runs the {@code onClosed} handler. */
    public void dismiss() {
        FadeTransition fade = new FadeTransition(Duration.millis(140), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            Parent parent = getParent();
            if (parent instanceof Pane) {
                ((Pane) parent).getChildren().remove(this);
            } else {
                setVisible(false);
                setManaged(false);
            }
            Runnable r = getOnClosed();
            if (r != null) {
                r.run();
            }
        });
        fade.play();
    }

    public final Severity getSeverity() {
        return severity.get();
    }

    public final void setSeverity(Severity severity) {
        this.severity.set(severity);
    }

    public final ObjectProperty<Severity> severityProperty() {
        return severity;
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

    public final String getMessage() {
        return message.get();
    }

    public final void setMessage(String message) {
        this.message.set(message == null ? "" : message);
    }

    public final StringProperty messageProperty() {
        return message;
    }

    /** True to show a close button. */
    public final boolean isClosable() {
        return closable.get();
    }

    public final void setClosable(boolean closable) {
        this.closable.set(closable);
    }

    public final BooleanProperty closableProperty() {
        return closable;
    }

    /** True to draw a soft drop shadow (used by snackbars). */
    public final boolean isElevated() {
        return elevated.get();
    }

    public final void setElevated(boolean elevated) {
        this.elevated.set(elevated);
    }

    public final BooleanProperty elevatedProperty() {
        return elevated;
    }

    /** A control shown to the right of the message (typically a button). */
    public final Node getAction() {
        return action.get();
    }

    public final void setAction(Node action) {
        this.action.set(action);
    }

    public final ObjectProperty<Node> actionProperty() {
        return action;
    }

    public final Runnable getOnClosed() {
        return onClosed.get();
    }

    public final void setOnClosed(Runnable onClosed) {
        this.onClosed.set(onClosed);
    }

    public final ObjectProperty<Runnable> onClosedProperty() {
        return onClosed;
    }
}
