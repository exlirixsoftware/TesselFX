package net.tessel.fx.components;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * A header that shows or hides a content area, with an animated chevron.
 *
 * <pre>{@code
 * Expander details = new Expander("Details", content, true);
 * }</pre>
 */
public class Expander extends VBox {

    private static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");

    private final StringProperty header = new SimpleStringProperty("");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>();
    private final BooleanProperty expanded = new SimpleBooleanProperty(false);

    private final HBox headerBar = new HBox();
    private final Label title = new Label();
    private final StackPane chevronHost = new StackPane();
    private final Icon chevron = new Icon(Symbol.CHEVRON_DOWN, 16);
    private final StackPane body = new StackPane();
    private final Collapsible collapsible = new Collapsible();
    private final RotateTransition rotate = new RotateTransition(Duration.millis(180), chevron);

    public Expander(String header, Node content, boolean expanded) {
        getStyleClass().add("tsl-expander");
        setFillWidth(true);

        headerBar.getStyleClass().add("tsl-expander-header");
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setFocusTraversable(true);
        title.getStyleClass().addAll("tsl-expander-title", "tsl-semibold");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);
        chevronHost.getStyleClass().add("tsl-expander-chevron");
        chevronHost.getChildren().add(chevron);
        headerBar.getChildren().addAll(title, chevronHost);
        title.textProperty().bind(this.header);

        body.getStyleClass().add("tsl-expander-body");
        body.setAlignment(Pos.TOP_LEFT);
        collapsible.setContent(body);
        getChildren().addAll(headerBar, collapsible);

        headerBar.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                setExpanded(!isExpanded());
                headerBar.requestFocus();
            }
        });
        headerBar.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                setExpanded(!isExpanded());
                e.consume();
            }
        });
        rotate.setInterpolator(Interpolator.EASE_OUT);

        this.content.addListener((o, a, b) -> {
            body.getChildren().clear();
            if (b != null) {
                body.getChildren().add(b);
            }
        });
        this.expanded.addListener((o, a, b) -> apply(b, true));

        setHeader(header);
        setContent(content);
        setExpanded(expanded);
        apply(expanded, false);
    }

    public Expander(String header, Node content) {
        this(header, content, false);
    }

    private void apply(boolean open, boolean animate) {
        pseudoClassStateChanged(EXPANDED, open);
        collapsible.set(open, animate);
        double angle = open ? 180 : 0;
        if (animate) {
            rotate.stop();
            rotate.setToAngle(angle);
            rotate.play();
        } else {
            rotate.stop();
            chevron.setRotate(angle);
        }
    }

    public final String getHeader() {
        return header.get();
    }

    public final void setHeader(String header) {
        this.header.set(header == null ? "" : header);
    }

    public final StringProperty headerProperty() {
        return header;
    }

    public final Node getContent() {
        return content.get();
    }

    public final void setContent(Node content) {
        this.content.set(content);
    }

    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    public final boolean isExpanded() {
        return expanded.get();
    }

    public final void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public final BooleanProperty expandedProperty() {
        return expanded;
    }
}
