package net.tessel.fx.gallery.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.tessel.fx.Tessel;
import net.tessel.fx.TextStyle;
import net.tessel.fx.components.Badge;
import net.tessel.fx.components.Card;
import net.tessel.fx.components.TextBlock;
import net.tessel.fx.gallery.Page;

public class HomePage extends Page {

    public HomePage() {
        super("Tessel for JavaFX",
                "A Fluent-inspired, themeable design system for JavaFX - the same look as Tessel.NET, Tessel4J and Tessel.js.");

        ImageView logo = new ImageView(new Image(HomePage.class.getResourceAsStream("/net/tessel/fx/gallery/logo.png")));
        logo.setFitWidth(72);
        logo.setFitHeight(72);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        Label version = Tessel.label("Version " + Tessel.VERSION, TextStyle.CAPTION);
        VBox about = new VBox(6, new Badge("JavaFX 21+", Badge.Kind.ACCENT), version);
        HBox hero = new HBox(20, logo, about);
        hero.setAlignment(Pos.CENTER_LEFT);

        FlowPane features = new FlowPane(16, 16,
                feature("Themes", "Light, dark and system themes, switchable at runtime, with any accent color."),
                feature("Every standard control", "Buttons, inputs, lists, tables, trees, menus, dialogs - restyled by one style sheet."),
                feature("Modern components", "NavigationView, TabView, ContentDialog, TeachingTip, Snackbar, InfoBar, NumberBox, ColorPickerPane and more."),
                feature("Tessel window", "A window with a themed title bar, rounded frame and shadow, in pure JavaFX."));
        features.setPrefWrapLength(900);

        TextBlock code = new TextBlock(
                "public void start(Stage stage) {\n"
                        + "    Tessel.setup(TesselTheme.SYSTEM);     // or LIGHT / DARK\n"
                        + "    stage.setScene(new Scene(root));\n"
                        + "    stage.show();\n"
                        + "}", TextStyle.CODE);
        add(hero, features, section("Getting started", code,
                new TextBlock("Every scene of every window - including menus, tool tips and dialogs - is themed "
                        + "automatically once Tessel is set up. Use Tessel.setTheme(...) and Tessel.setAccent(...) to change "
                        + "the look at runtime.", TextStyle.SECONDARY)));
    }

    private static Card feature(String title, String text) {
        Card card = new Card(title, new TextBlock(text, TextStyle.SECONDARY));
        card.setPrefWidth(280);
        card.setMinHeight(130);
        card.setPadding(new Insets(20));
        return card;
    }
}
