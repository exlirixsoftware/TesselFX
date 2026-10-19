package net.tessel.fx.gallery.pages;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import net.tessel.fx.Palette;
import net.tessel.fx.components.ColorPickerPane;
import net.tessel.fx.gallery.Page;

public class PickersPage extends Page {

    public PickersPage() {
        super("Pickers", "A color picker panel, plus the standard date and color pickers.");

        ColorPickerPane panel = new ColorPickerPane(Color.web("#F97316"));
        CheckBox alpha = new CheckBox("Opacity");
        alpha.selectedProperty().bindBidirectional(panel.alphaEnabledProperty());
        Label value = new Label();
        value.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> "Color: " + Palette.css(panel.getColor()), panel.colorProperty()));
        value.getStyleClass().add("tsl-text-caption");
        add(section("ColorPickerPane", row(panel), row(alpha), value));

        ColorPicker standard = new ColorPicker(Color.web("#4F46E5"));
        DatePicker date = new DatePicker(java.time.LocalDate.now());
        add(section("Standard pickers", row(standard, date)));
    }
}
