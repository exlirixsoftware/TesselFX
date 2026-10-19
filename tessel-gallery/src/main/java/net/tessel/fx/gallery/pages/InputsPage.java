package net.tessel.fx.gallery.pages;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import net.tessel.fx.Tessel;
import net.tessel.fx.components.NumberBox;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Symbol;

public class InputsPage extends Page {

    public InputsPage() {
        super("Inputs", "Text fields, number input with expressions, spinners, sliders and pickers.");

        TextField name = width(new TextField(), 220);
        name.setPromptText("Name");
        TextField search = width(new TextField(), 220);
        search.setPromptText("Search...");
        PasswordField password = width(new PasswordField(), 220);
        password.setPromptText("Password");
        TextField filled = width(new TextField("Some text"), 220);
        TextField disabled = width(new TextField("Disabled"), 220);
        disabled.setDisable(true);
        TextArea notes = new TextArea();
        notes.setPromptText("Write something...");
        notes.setPrefRowCount(3);
        add(section("Text",
                row(name, Tessel.withLeadingIcon(search, Symbol.SEARCH), password, filled, disabled),
                notes));

        NumberBox quantity = new NumberBox(1, 0, 99);
        quantity.setSuffix(" pcs");
        NumberBox compact = new NumberBox(2.5);
        compact.setSpinButtonPlacement(NumberBox.SpinButtonPlacement.COMPACT);
        compact.setSmallChange(0.5);
        NumberBox hidden = new NumberBox(10);
        hidden.setSpinButtonPlacement(NumberBox.SpinButtonPlacement.HIDDEN);
        NumberBox wrap = new NumberBox(11, 0, 11);
        wrap.setWrapEnabled(true);
        wrap.setPrefix("Month ");
        NumberBox empty = new NumberBox(0);
        empty.setNullable(true);
        empty.setValue(Double.NaN);
        empty.setPlaceholder("Optional");
        NumberBox money = new NumberBox(19.9);
        money.setPrefix("$");
        money.setFractionDigits(2);
        Label echo = new Label();
        echo.getStyleClass().add("tsl-text-caption");
        echo.textProperty().bind(quantity.valueProperty().asString("Quantity value: %.0f"));
        add(section("NumberBox",
                row(quantity, compact, hidden, wrap, empty, money),
                echo,
                caption("Type an expression such as  12 * 3 + 4  and press Enter. Arrow keys step, Page Up / Down take big steps, "
                        + "the mouse wheel steps while focused. Invalid text flashes red and restores the old value.")));

        Spinner<Integer> spinner = new Spinner<>(0, 20, 5);
        spinner.setEditable(true);
        Slider slider = width(new Slider(0, 100, 35), 280);
        Slider ticks = width(new Slider(0, 10, 4), 280);
        ticks.setShowTickMarks(true);
        ticks.setShowTickLabels(true);
        ticks.setMajorTickUnit(5);
        ticks.setMinorTickCount(4);
        ticks.setSnapToTicks(true);
        Slider vertical = new Slider(0, 100, 60);
        vertical.setOrientation(javafx.geometry.Orientation.VERTICAL);
        vertical.setPrefHeight(110);
        Slider off = width(new Slider(0, 100, 50), 280);
        off.setDisable(true);
        add(section("Spinner and sliders", row(spinner), row(new VBox(14, slider, ticks, off), vertical)));

        ComboBox<String> combo = width(new ComboBox<>(FXCollections.observableArrayList("Small", "Medium", "Large")), 200);
        combo.getSelectionModel().select(1);
        ComboBox<String> editable = width(new ComboBox<>(FXCollections.observableArrayList("Red", "Green", "Blue")), 200);
        editable.setEditable(true);
        editable.setPromptText("Type or pick");
        DatePicker date = new DatePicker(java.time.LocalDate.now());
        add(section("Pickers", row(combo, editable, date)));
    }
}
