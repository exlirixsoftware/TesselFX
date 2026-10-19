package net.tessel.fx.gallery.pages;

import javafx.geometry.Orientation;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.collections.FXCollections;
import javafx.scene.layout.StackPane;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.TextStyle;
import net.tessel.fx.components.Card;
import net.tessel.fx.components.Expander;
import net.tessel.fx.components.SettingsCard;
import net.tessel.fx.components.TextBlock;
import net.tessel.fx.components.ToggleSwitch;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Symbol;

public class LayoutPage extends Page {

    public LayoutPage() {
        super("Layout", "Cards, settings cards, expanders, split panes and titled panes.");

        Card plain = new Card("Profile", new TextBlock("A card groups related content on a surface with a rounded border.", TextStyle.SECONDARY),
                Tessel.style(new Button("Edit profile"), ButtonStyle.OUTLINE));
        plain.setPrefWidth(300);
        Card elevated = new Card("Elevated", new TextBlock("setElevated(true) adds a soft shadow.", TextStyle.SECONDARY));
        elevated.setElevated(true);
        elevated.setPrefWidth(300);
        add(section("Card", row(plain, elevated)));

        ComboBox<String> theme = new ComboBox<>(FXCollections.observableArrayList("Use system setting", "Light", "Dark"));
        theme.getSelectionModel().select(0);
        add(section("Settings card", column(8,
                new SettingsCard("Notifications", "Show alerts for mentions and replies", Symbol.RINGER, new ToggleSwitch(null, true)),
                new SettingsCard("Wi-Fi", "Connected to Home network", Symbol.WIFI, new ToggleSwitch(null, true)),
                new SettingsCard("Theme", "Choose how the app looks", Symbol.COLOR, theme),
                new SettingsCard("Storage", "12.4 GB of 64 GB used", Symbol.CLOUD, new Button("Manage")))));

        add(section("Expander", column(8,
                new Expander("Details", new TextBlock("The expander animates its height and rotates its chevron. Click the header or press Space."), true),
                new Expander("More options", new TextBlock("Hidden until you open it.")),
                new Expander("Advanced", new TextBlock("Anything can go in here: forms, lists, other cards...")))));

        StackPane left = pane("Left");
        StackPane right = pane("Right");
        SplitPane split = new SplitPane(left, right);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setPrefHeight(120);
        TitledPane t1 = new TitledPane("Section one", new Label("Content of the first section"));
        TitledPane t2 = new TitledPane("Section two", new Label("Content of the second section"));
        Accordion accordion = new Accordion(t1, t2);
        accordion.setExpandedPane(t1);
        add(section("Split pane, titled pane and accordion", split, accordion));
    }

    private static StackPane pane(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("tsl-text-secondary");
        return new StackPane(l);
    }
}
