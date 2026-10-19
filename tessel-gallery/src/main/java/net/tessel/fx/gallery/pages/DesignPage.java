package net.tessel.fx.gallery.pages;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.tessel.fx.Palette;
import net.tessel.fx.Tessel;
import net.tessel.fx.TextStyle;
import net.tessel.fx.components.Card;
import net.tessel.fx.components.TextBlock;
import net.tessel.fx.gallery.Page;

public class DesignPage extends Page {

    private final FlowPane colors = new FlowPane(12, 12);

    public DesignPage() {
        super("Design", "The Tessel palette, type ramp and shape language.");
        colors.setPrefWrapLength(900);
        Tessel.paletteProperty().addListener((o, a, b) -> fill(b));
        fill(Tessel.palette());
        add(section("Palette", colors,
                caption("Available to code as Tessel.palette().accent, ... and to style sheets as the looked-up colors -tsl-accent, ...")));

        VBox ramp = new VBox(10);
        for (TextStyle style : TextStyle.values()) {
            ramp.getChildren().add(new TextBlock(style.name() + "  -  " + (int) style.size() + " px", style));
        }
        add(section("Typography", ramp));

        FlowPane shapes = new FlowPane(16, 16,
                shape("4", "rows, menu items"), shape("6", "buttons, inputs"), shape("8", "cards, dialogs, menus"), shape("999", "badges, avatars"));
        add(section("Corner radius", shapes));
    }

    private void fill(Palette p) {
        colors.getChildren().setAll(
                swatch("background", p.background), swatch("surface", p.surface), swatch("surfaceAlt", p.surfaceAlt),
                swatch("subtle", p.subtle), swatch("border", p.border), swatch("borderStrong", p.borderStrong),
                swatch("text", p.text), swatch("textSecondary", p.textSecondary), swatch("textTertiary", p.textTertiary),
                swatch("controlHover", p.controlHover), swatch("accent", p.accent), swatch("accentHover", p.accentHover),
                swatch("accentSubtle", p.accentSubtle), swatch("success", p.success), swatch("warning", p.warning),
                swatch("danger", p.danger), swatch("info", p.info), swatch("successSubtle", p.successSubtle),
                swatch("warningSubtle", p.warningSubtle), swatch("dangerSubtle", p.dangerSubtle), swatch("infoSubtle", p.infoSubtle));
    }

    private static VBox swatch(String name, Color color) {
        Region chip = new Region();
        chip.setPrefSize(120, 44);
        chip.setMinSize(120, 44);
        chip.setStyle("-fx-background-color: " + Palette.css(color) + "; -fx-background-radius: 6; "
                + "-fx-border-color: -tsl-border; -fx-border-radius: 6;");
        Label label = new Label(name);
        label.getStyleClass().add("tsl-text-caption");
        Label hex = new Label(Palette.css(color).toUpperCase());
        hex.getStyleClass().addAll("tsl-text-caption", "tsl-mono");
        VBox box = new VBox(4, chip, label, hex);
        return box;
    }

    private static VBox shape(String radius, String where) {
        Region r = new Region();
        r.setPrefSize(88, 56);
        r.setMinSize(88, 56);
        double rad = Math.min(Double.parseDouble(radius), 28);
        r.setStyle("-fx-background-color: -tsl-accent-subtle; -fx-border-color: -tsl-accent; -fx-border-width: 1.5; "
                + "-fx-background-radius: " + rad + "; -fx-border-radius: " + rad + ";");
        Label a = new Label(radius);
        a.getStyleClass().add("tsl-text-body-strong");
        Label b = new Label(where);
        b.getStyleClass().add("tsl-text-caption");
        VBox box = new VBox(6, r, a, b);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }
}
