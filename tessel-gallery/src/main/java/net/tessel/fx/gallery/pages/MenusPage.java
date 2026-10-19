package net.tessel.fx.gallery.pages;

import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

public class MenusPage extends Page {

    public MenusPage() {
        super("Menus and tool bars", "Menu bar, context menu and tool tips.");

        MenuItem open = new MenuItem("Open...");
        open.setGraphic(new Icon(Symbol.FOLDER, 16));
        open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        MenuItem save = new MenuItem("Save");
        save.setGraphic(new Icon(Symbol.SAVE, 16));
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        Menu recent = new Menu("Open recent", null, new MenuItem("report.docx"), new MenuItem("notes.txt"));
        Menu file = new Menu("File", null, new MenuItem("New"), open, recent, new SeparatorMenuItem(), save,
                new SeparatorMenuItem(), new MenuItem("Exit"));
        CheckMenuItem ruler = new CheckMenuItem("Ruler");
        ruler.setSelected(true);
        CheckMenuItem grid = new CheckMenuItem("Grid");
        ToggleGroup zoom = new ToggleGroup();
        RadioMenuItem z100 = new RadioMenuItem("100%");
        z100.setToggleGroup(zoom);
        z100.setSelected(true);
        RadioMenuItem z200 = new RadioMenuItem("200%");
        z200.setToggleGroup(zoom);
        Menu view = new Menu("View", null, ruler, grid, new SeparatorMenuItem(), z100, z200);
        Menu help = new Menu("Help", null, new MenuItem("Documentation"), new MenuItem("About"));
        MenuBar bar = new MenuBar(file, view, help);
        add(section("Menu bar", bar));

        Button cut = Tessel.style(new Button("Cut"), ButtonStyle.SUBTLE);
        cut.setGraphic(new Icon(Symbol.CUT));
        Button copy = Tessel.style(new Button("Copy"), ButtonStyle.SUBTLE);
        copy.setGraphic(new Icon(Symbol.COPY));
        Button paste = Tessel.style(new Button("Paste"), ButtonStyle.SUBTLE);
        paste.setGraphic(new Icon(Symbol.PASTE));
        Button undo = Tessel.style(new Button(), ButtonStyle.ICON);
        undo.setGraphic(new Icon(Symbol.UNDO));
        Button redo = Tessel.style(new Button(), ButtonStyle.ICON);
        redo.setGraphic(new Icon(Symbol.REDO));
        undo.setTooltip(new Tooltip("Undo (Ctrl+Z)"));
        redo.setTooltip(new Tooltip("Redo (Ctrl+Y)"));
        ToolBar toolBar = new ToolBar(undo, redo, new Separator(), cut, copy, paste);
        add(section("Tool bar", toolBar, caption("Buttons that do not fit move into the overflow menu - resize the window to try it.")));

        MenuItem a = new MenuItem("Rename");
        a.setGraphic(new Icon(Symbol.RENAME, 16));
        MenuItem b = new MenuItem("Delete");
        b.setGraphic(new Icon(Symbol.DELETE, 16));
        ContextMenu menu = new ContextMenu(a, new MenuItem("Duplicate"), new SeparatorMenuItem(), b);
        Label target = new Label("Right-click here");
        target.getStyleClass().add("tsl-text-secondary");
        StackPane area = new StackPane(target);
        area.setMinHeight(90);
        area.getStyleClass().add("tsl-demo-area");
        area.setStyle("-fx-border-color: -tsl-border-strong; -fx-border-style: dashed; -fx-border-radius: 8; -fx-border-width: 1.5;");
        area.setOnContextMenuRequested(e -> menu.show(area, e.getScreenX(), e.getScreenY()));
        Button tip = new Button("Hover for a tool tip");
        tip.setTooltip(new Tooltip("Tool tips follow the theme, too."));
        add(section("Context menu and tool tip", area, row(tip)));
    }
}
