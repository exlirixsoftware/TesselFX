package net.tessel.fx.gallery.pages;

import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.TesselTheme;
import net.tessel.fx.components.SettingsCard;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Symbol;

public class SettingsPage extends Page {

    private static final String[] ACCENTS = {
            "#4F46E5", "#0078D4", "#0891B2", "#059669", "#65A30D", "#D97706", "#DC2626", "#DB2777", "#7C3AED"
    };

    public SettingsPage() {
        super("Settings", "Theme and accent color of this gallery - the whole window follows.");

        ToggleGroup group = new ToggleGroup();
        HBox themes = new HBox(16);
        for (TesselTheme t : TesselTheme.values()) {
            RadioButton r = new RadioButton(t == TesselTheme.SYSTEM ? "System" : t == TesselTheme.LIGHT ? "Light" : "Dark");
            r.setToggleGroup(group);
            r.setSelected(Tessel.getTheme() == t);
            r.setOnAction(e -> Tessel.setTheme(t));
            themes.getChildren().add(r);
        }

        HBox accents = new HBox(8);
        for (String code : ACCENTS) {
            Button b = new Button();
            b.getStyleClass().add("gallery-accent");
            b.setStyle("-fx-background-color: " + code + "; -fx-background-radius: 14; -fx-min-width: 28; -fx-min-height: 28; "
                    + "-fx-pref-width: 28; -fx-pref-height: 28; -fx-max-width: 28; -fx-max-height: 28; -fx-padding: 0; "
                    + "-fx-background-insets: 0;");
            b.setOnAction(e -> Tessel.setAccent(Color.web(code)));
            accents.getChildren().add(b);
        }
        Button system = new Button("Windows accent");
        system.setOnAction(e -> Tessel.useSystemAccent());
        Button reset = Tessel.style(new Button("Default"), ButtonStyle.SUBTLE);
        reset.setOnAction(e -> Tessel.setAccent(null));
        accents.getChildren().addAll(system, reset);

        add(section("Appearance",
                new SettingsCard("Theme", "System follows the operating system and changes with it", Symbol.BRIGHTNESS, themes),
                new SettingsCard("Accent color", "Used for buttons, selection, focus and progress", Symbol.COLOR, accents)));

        add(section("About",
                new SettingsCard("Tessel for JavaFX", "Version " + Tessel.VERSION + " - MIT License", Symbol.INFO, null)));
    }
}
