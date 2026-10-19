package net.tessel.fx.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A surface with a rounded border and an optional header. Children added to the card are laid out below the header.
 *
 * <pre>{@code
 * Card card = new Card("Profile", form);
 * card.setElevated(true);   // soft shadow
 * }</pre>
 */
public class Card extends VBox {

    private final Label headerLabel = new Label();
    private final StringProperty header = new SimpleStringProperty("");
    private final BooleanProperty elevated = new SimpleBooleanProperty(false);

    public Card() {
        this(null);
    }

    public Card(String header, Node... content) {
        getStyleClass().add("tsl-card");
        headerLabel.getStyleClass().addAll("tsl-card-header", "tsl-semibold");
        headerLabel.textProperty().bind(this.header);
        headerLabel.managedProperty().bind(headerLabel.visibleProperty());
        headerLabel.visibleProperty().bind(this.header.isNotEmpty());
        getChildren().add(headerLabel);
        setHeader(header);
        getChildren().addAll(content);
        elevated.addListener((o, a, b) -> {
            if (b) {
                getStyleClass().add("tsl-elevated");
            } else {
                getStyleClass().remove("tsl-elevated");
            }
        });
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

    /** True to draw a soft drop shadow. */
    public final boolean isElevated() {
        return elevated.get();
    }

    public final void setElevated(boolean elevated) {
        this.elevated.set(elevated);
    }

    public final BooleanProperty elevatedProperty() {
        return elevated;
    }
}
