package net.tessel.fx.gallery;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import net.tessel.fx.TextStyle;
import net.tessel.fx.components.Card;
import net.tessel.fx.components.TextBlock;

/** The base of a gallery page: a scrolling column with a title and description, and a few layout helpers. */
public abstract class Page extends ScrollPane {
    private final VBox content = new VBox(20);

    protected Page(String title, String description) {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        content.setPadding(new Insets(24, 32, 32, 32));
        VBox head = new VBox(4, new TextBlock(title, TextStyle.TITLE), new TextBlock(description, TextStyle.SECONDARY));
        content.getChildren().add(head);
        setContent(content);
    }

    protected final void add(Node... nodes) {
        content.getChildren().addAll(nodes);
    }

    /** A card with a header that holds the demo controls. */
    protected static Card section(String title, Node... body) {
        Card card = new Card(title, body);
        card.setSpacing(14);
        return card;
    }

    /** Controls side by side, wrapping when the page is narrow. */
    protected static FlowPane row(Node... nodes) {
        FlowPane pane = new FlowPane(12, 12, nodes);
        pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return pane;
    }

    protected static VBox column(double gap, Node... nodes) {
        return new VBox(gap, nodes);
    }

    protected static Label caption(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("tsl-text-caption");
        l.setWrapText(true);
        l.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return l;
    }

    protected static <T extends javafx.scene.layout.Region> T width(T node, double width) {
        node.setPrefWidth(width);
        node.setMinWidth(width);
        return node;
    }
}
