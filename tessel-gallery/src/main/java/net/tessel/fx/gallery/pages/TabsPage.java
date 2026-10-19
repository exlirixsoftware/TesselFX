package net.tessel.fx.gallery.pages;

import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import net.tessel.fx.components.BreadcrumbBar;
import net.tessel.fx.components.TabView;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

public class TabsPage extends Page {

    private int untitled = 1;

    public TabsPage() {
        super("Tabs and navigation", "Document tabs, tab pane, breadcrumb bar and pagination.");

        TabView tabs = new TabView();
        tabs.addTab("Readme.md", new Icon(Symbol.DOCUMENT, 16), editor("# Tessel\n\nDrag the tabs to reorder them. Middle-click or Ctrl+W closes a tab."));
        tabs.addTab("Main.java", new Icon(Symbol.CODE, 16), editor("public class Main { }"));
        tabs.addTab("Notes", new Icon(Symbol.PAGE, 16), editor("Shopping list"));
        tabs.setAddTabHandler(() -> tabs.addTab("Untitled " + untitled++, editor("")));
        tabs.select(0);
        tabs.setPrefHeight(230);
        add(section("TabView", tabs,
                caption("Closable and reorderable. Tabs shrink to fit, then scroll with arrow buttons or the mouse wheel. "
                        + "Ctrl+PageUp / Ctrl+PageDown switch tabs.")));

        TabPane pane = new TabPane(new Tab("General", new Label("General settings")), new Tab("Privacy", new Label("Privacy settings")),
                new Tab("Advanced", new Label("Advanced settings")));
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pane.setPrefHeight(120);
        add(section("TabPane (standard control)", pane));

        BreadcrumbBar crumbs = new BreadcrumbBar("Home", "Documents", "Projects", "Tessel", "Reports", "2026");
        crumbs.setTruncateOnClick(true);
        Label where = new Label("Click an item - it becomes the current location.");
        where.getStyleClass().add("tsl-text-caption");
        crumbs.addItemClickListener((bar, index, item) -> where.setText("Opened: " + item));
        crumbs.setItemIconProvider(item -> "Home".equals(item) ? new Icon(Symbol.HOME, 14) : null);
        add(section("BreadcrumbBar", crumbs, where,
                caption("When the bar is too narrow the first items collapse into a \"...\" menu - make the window smaller.")));

        Pagination pagination = new Pagination(7, 2);
        pagination.setPageFactory(i -> new Label("Page " + (i + 1)));
        pagination.setPrefHeight(120);
        add(section("Pagination", pagination));
    }

    private static TextArea editor(String text) {
        TextArea area = new TextArea(text);
        area.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        return area;
    }
}
