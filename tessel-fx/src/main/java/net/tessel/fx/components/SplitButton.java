package net.tessel.fx.components;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitMenuButton;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;

/**
 * A button with a main action and a drop-down menu of alternatives. It is a {@link SplitMenuButton}; the menu is
 * its item list.
 *
 * <pre>{@code
 * SplitButton save = Tessel.style(new SplitButton("Save", new Icon(Symbol.SAVE)), ButtonStyle.ACCENT);
 * save.setOnAction(e -> save());                    // the main part
 * save.addMenuItem("Save as...", () -> saveAs());   // the arrow part
 * }</pre>
 */
public class SplitButton extends SplitMenuButton {

    public SplitButton(String text) {
        this(text, null);
    }

    public SplitButton(String text, Node graphic) {
        super();
        setText(text);
        setGraphic(graphic);
        getStyleClass().add("tsl-split-button");
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
    public SplitButton withStyle(ButtonStyle style) {
        return Tessel.style(this, style);
    }
}
