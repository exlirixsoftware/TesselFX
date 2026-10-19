package net.tessel.fx.gallery.pages;

import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.components.DropDownButton;
import net.tessel.fx.components.SplitButton;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

public class ButtonsPage extends Page {

    public ButtonsPage() {
        super("Buttons", "Seven button styles, icon buttons, toggles, split and drop-down buttons.");

        Button icon = Tessel.style(new Button(), ButtonStyle.ICON);
        icon.setGraphic(new Icon(Symbol.SETTINGS));
        add(section("Styles",
                row(new Button("Standard"),
                        Tessel.style(new Button("Accent"), ButtonStyle.ACCENT),
                        Tessel.style(new Button("Outline"), ButtonStyle.OUTLINE),
                        Tessel.style(new Button("Subtle"), ButtonStyle.SUBTLE),
                        Tessel.style(new Button("Danger"), ButtonStyle.DANGER),
                        icon,
                        Tessel.style(new Button("Link"), ButtonStyle.LINK)),
                caption("Tessel.style(button, ButtonStyle.ACCENT)  -  STANDARD, ACCENT, OUTLINE, SUBTLE, DANGER, ICON, LINK")));

        Button save = Tessel.style(new Button("Save"), ButtonStyle.ACCENT);
        save.setGraphic(new Icon(Symbol.SAVE));
        Button share = new Button("Share");
        share.setGraphic(new Icon(Symbol.SHARE));
        Button delete = Tessel.style(new Button("Delete"), ButtonStyle.DANGER);
        delete.setGraphic(new Icon(Symbol.DELETE));
        Button disabled = new Button("Disabled");
        disabled.setDisable(true);
        Button disabledAccent = Tessel.style(new Button("Disabled"), ButtonStyle.ACCENT);
        disabledAccent.setDisable(true);
        add(section("With icons, and disabled", row(save, share, delete, disabled, disabledAccent)));

        ToggleButton a = new ToggleButton("Day");
        ToggleButton b = new ToggleButton("Week");
        ToggleButton c = new ToggleButton("Month");
        ToggleGroup group = new ToggleGroup();
        a.setToggleGroup(group);
        b.setToggleGroup(group);
        c.setToggleGroup(group);
        b.setSelected(true);
        ToggleButton bold = new ToggleButton();
        bold.setGraphic(new Icon(Symbol.FONT));
        add(section("Toggle buttons", row(new HBox(6, a, b, c), bold)));

        SplitButton splitSave = Tessel.style(new SplitButton("Save", new Icon(Symbol.SAVE)), ButtonStyle.ACCENT);
        splitSave.addMenuItem("Save as...", () -> { });
        splitSave.addMenuItem("Save a copy", () -> { });
        splitSave.addSeparator();
        splitSave.addMenuItem("Save all", () -> { });
        SplitButton splitPlain = new SplitButton("Open");
        splitPlain.addMenuItem("Open recent", () -> { });
        splitPlain.addMenuItem("Open from URL...", () -> { });
        DropDownButton sort = new DropDownButton("Sort by", new Icon(Symbol.SORT));
        sort.addMenuItem("Name", () -> { });
        sort.addMenuItem("Date modified", () -> { });
        sort.addMenuItem("Size", () -> { });
        DropDownButton more = Tessel.style(new DropDownButton("More"), ButtonStyle.SUBTLE);
        more.addMenuItem("Rename", () -> { });
        more.addMenuItem("Delete", () -> { });
        add(section("Split button and drop-down button",
                row(splitSave, splitPlain, sort, more),
                caption("SplitButton: the main part runs the action, the arrow opens the menu. "
                        + "DropDownButton: the whole button opens the menu.")));

        Hyperlink link = new Hyperlink("A hyperlink");
        add(section("Hyperlink", row(link)));
    }
}
