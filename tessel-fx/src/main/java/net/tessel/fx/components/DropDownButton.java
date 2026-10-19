package net.tessel.fx.components;

import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;

/**
 * A button whose whole surface opens a menu, with a chevron at the end.
 *
 * <pre>{@code
 * DropDownButton sort = new DropDownButton("Sort by");
 * sort.addMenuItem("Name", () -> sortBy(NAME));
 * }</pre>
 */
public class DropDownButton extends MenuButton {

    public DropDownButton(String text) {
        this(text, null);
    }

    public DropDownButton(String text, Node graphic) {
        super();
        setText(text);
        setGraphic(graphic);
        getStyleClass().add("tsl-drop-down-button");
        setPopupSide(javafx.geometry.Side.BOTTOM);
        setMnemonicParsing(false);
    }

    /** Adds a menu entry that runs the action. */
    public MenuItem addMenuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setMnemonicParsing(false);
        item.setOnAction(e -> action.run());
        getItems().add(item);
        return item;
    }

    public void addSeparator() {
        getItems().add(new SeparatorMenuItem());
    }

    /** Convenience: sets the {@link ButtonStyle} of this button. */
    public DropDownButton withStyle(ButtonStyle style) {
        return Tessel.style(this, style);
    }
}
