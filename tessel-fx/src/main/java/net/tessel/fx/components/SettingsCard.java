package net.tessel.fx.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * A settings row: an icon, a title with a description, and a control on the right (a switch, a combo box, a button...).
 *
 * <pre>{@code
 * new SettingsCard("Notifications", "Alerts for mentions", Symbol.RINGER, new ToggleSwitch());
 * }</pre>
 */
public class SettingsCard extends HBox {

    private final StringProperty header = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final ObjectProperty<Symbol> symbol = new SimpleObjectProperty<>();
    private final ObjectProperty<Node> control = new SimpleObjectProperty<>();

    private final StackPane iconHost = new StackPane();
    private final StackPane controlHost = new StackPane();
    private final Label headerLabel = new Label();
    private final Label descriptionLabel = new Label();

    public SettingsCard(String header, String description, Symbol symbol, Node control) {
        getStyleClass().add("tsl-settings-card");
        setAlignment(Pos.CENTER_LEFT);

        iconHost.getStyleClass().add("tsl-settings-icon");
        headerLabel.getStyleClass().add("tsl-settings-title");
        descriptionLabel.getStyleClass().add("tsl-settings-description");
        descriptionLabel.setWrapText(true);
        controlHost.getStyleClass().add("tsl-settings-control");
        controlHost.setAlignment(Pos.CENTER_RIGHT);

        VBox text = new VBox(2, headerLabel, descriptionLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        getChildren().addAll(iconHost, text, controlHost);

        headerLabel.textProperty().bind(this.header);
        descriptionLabel.textProperty().bind(this.description);
        descriptionLabel.managedProperty().bind(descriptionLabel.visibleProperty());
        descriptionLabel.visibleProperty().bind(this.description.isNotEmpty());
        iconHost.managedProperty().bind(iconHost.visibleProperty());
        iconHost.setVisible(false);
        this.symbol.addListener((o, a, b) -> {
            iconHost.getChildren().clear();
            iconHost.setVisible(b != null);
            if (b != null) {
                iconHost.getChildren().add(new Icon(b, 20));
            }
        });
        this.control.addListener((o, a, b) -> {
            controlHost.getChildren().clear();
            if (b != null) {
                controlHost.getChildren().add(b);
            }
        });

        setHeader(header);
        setDescription(description);
        setSymbol(symbol);
        setControl(control);
    }

    public SettingsCard(String header, String description) {
        this(header, description, null, null);
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

    public final String getDescription() {
        return description.get();
    }

    public final void setDescription(String description) {
        this.description.set(description == null ? "" : description);
    }

    public final StringProperty descriptionProperty() {
        return description;
    }

    public final Symbol getSymbol() {
        return symbol.get();
    }

    public final void setSymbol(Symbol symbol) {
        this.symbol.set(symbol);
    }

    public final ObjectProperty<Symbol> symbolProperty() {
        return symbol;
    }

    public final Node getControl() {
        return control.get();
    }

    public final void setControl(Node control) {
        this.control.set(control);
    }

    public final ObjectProperty<Node> controlProperty() {
        return control;
    }
}
