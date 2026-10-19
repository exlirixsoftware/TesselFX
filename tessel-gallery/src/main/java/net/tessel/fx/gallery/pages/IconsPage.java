package net.tessel.fx.gallery.pages;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.tessel.fx.Tessel;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

public class IconsPage extends Page {

    public IconsPage() {
        super("Icons", "Vector glyphs that follow the text color - they look the same on every operating system.");

        TextField search = width(new TextField(), 280);
        search.setPromptText("Filter icons");
        FlowPane grid = new FlowPane(8, 8);
        grid.setPrefWrapLength(900);
        for (Symbol symbol : Symbol.values()) {
            Icon icon = new Icon(symbol, 24);
            Label name = new Label(symbol.name().toLowerCase().replace('_', ' '));
            name.getStyleClass().add("tsl-text-caption");
            name.setMaxWidth(100);
            VBox tile = new VBox(8, icon, name);
            tile.setAlignment(Pos.CENTER);
            tile.setPrefSize(110, 76);
            tile.setMinSize(110, 76);
            tile.getStyleClass().add("gallery-icon-tile");
            tile.setStyle("-fx-background-color: -tsl-surface-alt; -fx-background-radius: 8; -fx-border-color: -tsl-border; "
                    + "-fx-border-radius: 8;");
            tile.setUserData(symbol.name().toLowerCase());
            grid.getChildren().add(tile);
        }
        search.textProperty().addListener((o, a, text) -> {
            String q = text.trim().toLowerCase().replace(' ', '_');
            for (javafx.scene.Node n : grid.getChildren()) {
                boolean show = q.isEmpty() || ((String) n.getUserData()).contains(q);
                n.setVisible(show);
                n.setManaged(show);
            }
        });
        StackPane holder = new StackPane(grid);
        holder.setAlignment(Pos.TOP_LEFT);
        add(Tessel.withLeadingIcon(search, Symbol.SEARCH), holder);
    }
}
