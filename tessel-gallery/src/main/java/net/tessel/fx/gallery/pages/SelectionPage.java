package net.tessel.fx.gallery.pages;

import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import net.tessel.fx.components.ToggleSwitch;
import net.tessel.fx.gallery.Page;

public class SelectionPage extends Page {

    public SelectionPage() {
        super("Selection", "Check boxes, radio buttons, switches and choice boxes.");

        CheckBox checked = new CheckBox("Checked");
        checked.setSelected(true);
        CheckBox unchecked = new CheckBox("Unchecked");
        CheckBox indeterminate = new CheckBox("Indeterminate");
        indeterminate.setIndeterminate(true);
        CheckBox tri = new CheckBox("Three states (click me)");
        tri.setAllowIndeterminate(true);
        tri.setIndeterminate(true);
        CheckBox disabled = new CheckBox("Disabled");
        disabled.setSelected(true);
        disabled.setDisable(true);
        add(section("Check box", row(checked, unchecked, indeterminate, tri, disabled)));

        ToggleGroup group = new ToggleGroup();
        RadioButton a = new RadioButton("Small");
        RadioButton b = new RadioButton("Medium");
        RadioButton c = new RadioButton("Large");
        for (RadioButton r : new RadioButton[]{a, b, c}) {
            r.setToggleGroup(group);
        }
        b.setSelected(true);
        RadioButton d = new RadioButton("Disabled");
        d.setDisable(true);
        add(section("Radio button", row(a, b, c, d)));

        ToggleSwitch wifi = new ToggleSwitch("Wi-Fi", true);
        ToggleSwitch bluetooth = new ToggleSwitch("Bluetooth", false);
        ToggleSwitch airplane = new ToggleSwitch("Airplane mode (disabled)", false);
        airplane.setDisable(true);
        ToggleSwitch plain = new ToggleSwitch();
        add(section("Toggle switch", row(wifi, bluetooth, airplane, plain)));

        ChoiceBox<String> choice = new ChoiceBox<>(FXCollections.observableArrayList("Alpha", "Beta", "Gamma"));
        choice.getSelectionModel().select(0);
        add(section("Choice box", new VBox(choice)));
    }
}
