package net.tessel.fx.components;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * Closable, reorderable document tabs.
 *
 * <pre>{@code
 * TabView tabs = new TabView();
 * tabs.addTab("Readme.md", new Icon(Symbol.DOCUMENT, 16), new TextArea());
 * tabs.setAddTabHandler(() -> tabs.addTab("Untitled", new TextArea()));   // shows the + button
 * tabs.addTabViewListener(new TabView.TabViewListener() {
 *     public boolean tabClosing(TabView view, TabView.Tab tab) { return confirmClose(tab); }   // false vetoes
 * });
 * }</pre>
 *
 * Drag tabs to reorder ({@link #setReorderable}), middle-click or Ctrl+W to close, Ctrl+PageUp / Ctrl+PageDown to
 * switch. Tabs shrink to fit and then scroll with arrow buttons or the mouse wheel.
 */
public class TabView extends BorderPane {

    /** One tab of a {@link TabView}. */
    public static final class Tab {
        private final StringProperty title = new SimpleStringProperty("");
        private final ObjectProperty<Node> icon = new SimpleObjectProperty<>();
        private final ObjectProperty<Node> content = new SimpleObjectProperty<>();
        private final BooleanProperty closable = new SimpleBooleanProperty(true);
        private final StringProperty tooltip = new SimpleStringProperty("");
        private Object data;

        private Tab(String title, Node icon, Node content) {
            this.title.set(title == null ? "" : title);
            this.icon.set(icon);
            this.content.set(content);
        }

        public String getTitle() {
            return title.get();
        }

        public void setTitle(String title) {
            this.title.set(title == null ? "" : title);
        }

        public StringProperty titleProperty() {
            return title;
        }

        public Node getIcon() {
            return icon.get();
        }

        public void setIcon(Node icon) {
            this.icon.set(icon);
        }

        public ObjectProperty<Node> iconProperty() {
            return icon;
        }

        public Node getContent() {
            return content.get();
        }

        public void setContent(Node content) {
            this.content.set(content);
        }

        public ObjectProperty<Node> contentProperty() {
            return content;
        }

        public boolean isClosable() {
            return closable.get();
        }

        public void setClosable(boolean closable) {
            this.closable.set(closable);
        }

        public BooleanProperty closableProperty() {
            return closable;
        }

        public String getTooltip() {
            return tooltip.get();
        }

        public void setTooltip(String tooltip) {
            this.tooltip.set(tooltip == null ? "" : tooltip);
        }

        public StringProperty tooltipProperty() {
            return tooltip;
        }

        /** Free-form data attached to the tab. */
        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    /** Reports what happens to the tabs. Override only what you need. */
    public interface TabViewListener {
        default void selectionChanged(TabView view, Tab tab) {
        }

        /** @return false to keep the tab open */
        default boolean tabClosing(TabView view, Tab tab) {
            return true;
        }

        default void tabClosed(TabView view, Tab tab) {
        }

        default void tabMoved(TabView view, Tab tab, int from, int to) {
        }
    }

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final double HEIGHT = 38;
    private static final double MIN_TAB = 72;
    private static final double MAX_TAB = 220;
    private static final double ADD_WIDTH = 34;
    private static final double ARROW_WIDTH = 26;
    private static final double PAD = 8;

    private final ObservableList<Tab> tabs = FXCollections.observableArrayList();
    private final ObjectProperty<Tab> selectedTab = new SimpleObjectProperty<>();
    private final BooleanProperty reorderable = new SimpleBooleanProperty(true);
    private final ObjectProperty<Runnable> addTabHandler = new SimpleObjectProperty<>();
    private final List<TabViewListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Tab, TabHeader> headers = new IdentityHashMap<>();
    private final Strip strip = new Strip();
    private final StackPane contentHost = new StackPane();

    public TabView() {
        getStyleClass().add("tsl-tab-view");
        contentHost.getStyleClass().add("tsl-tab-content");
        setTop(strip);
        setCenter(contentHost);
        // the strip overlaps the top border of the content by a pixel so the selected tab joins the page
        strip.setViewOrder(-1);
        strip.setTranslateY(1);

        selectedTab.addListener((o, a, b) -> {
            if (a != null && headers.containsKey(a)) {
                headers.get(a).pseudoClassStateChanged(SELECTED, false);
            }
            if (b != null && headers.containsKey(b)) {
                headers.get(b).pseudoClassStateChanged(SELECTED, true);
            }
            contentHost.getChildren().setAll(b == null || b.getContent() == null ? new Node[0] : new Node[]{b.getContent()});
            strip.reveal = true;
            strip.requestLayout();
            for (TabViewListener l : listeners) {
                l.selectionChanged(this, b);
            }
        });
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && (e.getCode() == KeyCode.W || e.getCode() == KeyCode.F4)) {
                closeTab(getSelectedTab());
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.PAGE_UP) {
                cycle(-1);
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.PAGE_DOWN) {
                cycle(1);
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.TAB) {
                cycle(e.isShiftDown() ? -1 : 1);
                e.consume();
            }
        });
    }

    // ------------------------------------------------------------------ tabs

    public Tab addTab(String title, Node content) {
        return addTab(title, null, content);
    }

    public Tab addTab(String title, Node icon, Node content) {
        return insertTab(tabs.size(), title, icon, content);
    }

    public Tab insertTab(int index, String title, Node icon, Node content) {
        Tab tab = new Tab(title, icon, content);
        TabHeader header = new TabHeader(tab);
        headers.put(tab, header);
        tabs.add(index, tab);
        strip.viewport.getChildren().add(header);
        tab.content.addListener((o, a, b) -> {
            if (getSelectedTab() == tab) {
                contentHost.getChildren().setAll(b == null ? new Node[0] : new Node[]{b});
            }
        });
        strip.requestLayout();
        select(tab);
        return tab;
    }

    /** Closes a tab, unless it is not closable or a listener vetoes it. @return true when the tab was closed */
    public boolean closeTab(Tab tab) {
        if (tab == null || !tabs.contains(tab) || !tab.isClosable()) {
            return false;
        }
        for (TabViewListener l : listeners) {
            if (!l.tabClosing(this, tab)) {
                return false;
            }
        }
        int at = tabs.indexOf(tab);
        boolean wasSelected = getSelectedTab() == tab;
        tabs.remove(tab);
        TabHeader header = headers.remove(tab);
        strip.viewport.getChildren().remove(header);
        if (wasSelected) {
            selectedTab.set(tabs.isEmpty() ? null : tabs.get(Math.min(at, tabs.size() - 1)));
            if (getSelectedTab() != null) {
                headers.get(getSelectedTab()).requestFocus();
            }
        }
        strip.requestLayout();
        for (TabViewListener l : listeners) {
            l.tabClosed(this, tab);
        }
        return true;
    }

    public void select(Tab tab) {
        if (tab != null && tabs.contains(tab)) {
            selectedTab.set(tab);
        }
    }

    public void select(int index) {
        if (index >= 0 && index < tabs.size()) {
            select(tabs.get(index));
        }
    }

    private void cycle(int delta) {
        if (tabs.isEmpty()) {
            return;
        }
        int at = Math.max(0, tabs.indexOf(getSelectedTab()));
        Tab next = tabs.get((at + delta + tabs.size()) % tabs.size());
        select(next);
        headers.get(next).requestFocus();
    }

    private void move(Tab tab, int to) {
        int from = tabs.indexOf(tab);
        if (from < 0 || from == to) {
            return;
        }
        tabs.remove(from);
        tabs.add(to, tab);
        strip.requestLayout();
    }

    // ------------------------------------------------------------------ properties

    /** The tabs in order. Use the methods of the view to add, close or move tabs. */
    public final List<Tab> getTabs() {
        return java.util.Collections.unmodifiableList(tabs);
    }

    public final Tab getSelectedTab() {
        return selectedTab.get();
    }

    public final ObjectProperty<Tab> selectedTabProperty() {
        return selectedTab;
    }

    public final boolean isReorderable() {
        return reorderable.get();
    }

    public final void setReorderable(boolean reorderable) {
        this.reorderable.set(reorderable);
    }

    public final BooleanProperty reorderableProperty() {
        return reorderable;
    }

    /** Called when the + button is pressed; the button is only shown while a handler is set. */
    public final Runnable getAddTabHandler() {
        return addTabHandler.get();
    }

    public final void setAddTabHandler(Runnable handler) {
        addTabHandler.set(handler);
        strip.addButton.setVisible(handler != null);
        strip.requestLayout();
    }

    public final ObjectProperty<Runnable> addTabHandlerProperty() {
        return addTabHandler;
    }

    public final void addTabViewListener(TabViewListener listener) {
        listeners.add(listener);
    }

    public final void removeTabViewListener(TabViewListener listener) {
        listeners.remove(listener);
    }

    // ------------------------------------------------------------------ header

    private final class TabHeader extends HBox {
        private final Tab tab;
        private double grab;
        private double pressSceneX;
        private boolean dragging;
        private int origin;
        private double dragLeft;

        TabHeader(Tab tab) {
            this.tab = tab;
            getStyleClass().add("tsl-tab");
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(true);
            Label title = new Label();
            title.textProperty().bind(tab.title);
            title.getStyleClass().add("tsl-tab-title");
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(title, Priority.ALWAYS);
            StackPane iconHost = new StackPane();
            iconHost.getStyleClass().add("tsl-tab-icon");
            iconHost.managedProperty().bind(iconHost.visibleProperty());
            Runnable setIcon = () -> {
                iconHost.getChildren().clear();
                Node icon = tab.getIcon();
                iconHost.setVisible(icon != null);
                if (icon != null) {
                    iconHost.getChildren().add(icon);
                }
            };
            tab.icon.addListener((o, a, b) -> setIcon.run());
            setIcon.run();
            Button close = Tessel.style(new Button(), ButtonStyle.ICON);
            close.getStyleClass().add("tsl-tab-close");
            close.setGraphic(new Icon(Symbol.CHROME_CLOSE, 10));
            close.setFocusTraversable(false);
            close.managedProperty().bind(close.visibleProperty());
            close.visibleProperty().bind(tab.closable);
            close.setOnAction(e -> closeTab(tab));
            getChildren().addAll(iconHost, title, close);

            Tooltip tip = new Tooltip();
            tip.textProperty().bind(tab.tooltip);
            tab.tooltip.addListener((o, a, b) -> {
                if (b.isEmpty()) {
                    Tooltip.uninstall(this, tip);
                } else {
                    Tooltip.install(this, tip);
                }
            });

            setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    select(tab);
                    requestFocus();
                    grab = e.getX();
                    pressSceneX = e.getSceneX();
                    origin = tabs.indexOf(tab);
                    dragging = false;
                } else if (e.getButton() == MouseButton.MIDDLE) {
                    closeTab(tab);
                    e.consume();
                }
            });
            setOnMouseDragged(e -> {
                if (e.getButton() != MouseButton.PRIMARY || !isReorderable() || tabs.size() < 2) {
                    return;
                }
                if (!dragging && Math.abs(e.getSceneX() - pressSceneX) > 4) {
                    dragging = true;
                    toFront();
                    strip.dragging = this;
                }
                if (dragging) {
                    double pointer = strip.viewport.sceneToLocal(e.getSceneX(), 0).getX();
                    dragLeft = pointer - grab;
                    int target = (int) Math.floor((pointer + strip.offset) / strip.tabWidth);
                    target = Math.max(0, Math.min(tabs.size() - 1, target));
                    move(tab, target);
                    strip.requestLayout();
                }
            });
            setOnMouseReleased(e -> {
                if (dragging) {
                    dragging = false;
                    strip.dragging = null;
                    int to = tabs.indexOf(tab);
                    strip.requestLayout();
                    if (to != origin) {
                        for (TabViewListener l : listeners) {
                            l.tabMoved(TabView.this, tab, origin, to);
                        }
                    }
                }
            });
            setOnKeyPressed(e -> {
                int at = tabs.indexOf(tab);
                if (e.getCode() == KeyCode.LEFT) {
                    cycleTo(at - 1);
                } else if (e.getCode() == KeyCode.RIGHT) {
                    cycleTo(at + 1);
                } else if (e.getCode() == KeyCode.HOME) {
                    cycleTo(0);
                } else if (e.getCode() == KeyCode.END) {
                    cycleTo(tabs.size() - 1);
                } else if (e.getCode() == KeyCode.DELETE) {
                    closeTab(tab);
                } else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                    select(tab);
                } else {
                    return;
                }
                e.consume();
            });
        }

        private void cycleTo(int index) {
            if (index >= 0 && index < tabs.size()) {
                Tab t = tabs.get(index);
                select(t);
                headers.get(t).requestFocus();
            }
        }
    }

    // ------------------------------------------------------------------ strip

    private final class Strip extends Region {
        final Pane viewport = new Pane() {
            @Override
            protected void layoutChildren() {
                // the strip places and sizes the tab headers itself
            }
        };
        final Button addButton = Tessel.style(new Button(), ButtonStyle.ICON);
        final Button left = Tessel.style(new Button(), ButtonStyle.ICON);
        final Button right = Tessel.style(new Button(), ButtonStyle.ICON);
        final Rectangle clip = new Rectangle();
        double offset;
        double tabWidth = MIN_TAB;
        boolean reveal;
        TabHeader dragging;

        Strip() {
            getStyleClass().add("tsl-tab-strip");
            viewport.setClip(clip);
            viewport.setPickOnBounds(false);
            addButton.getStyleClass().add("tsl-tab-add");
            addButton.setGraphic(new Icon(Symbol.ADD, 14));
            addButton.setFocusTraversable(false);
            addButton.setVisible(false);
            addButton.setOnAction(e -> {
                Runnable r = getAddTabHandler();
                if (r != null) {
                    r.run();
                }
            });
            left.getStyleClass().add("tsl-tab-scroll");
            left.setGraphic(new Icon(Symbol.CHEVRON_LEFT, 12));
            left.setFocusTraversable(false);
            left.setOnAction(e -> scrollBy(-tabWidth));
            right.getStyleClass().add("tsl-tab-scroll");
            right.setGraphic(new Icon(Symbol.CHEVRON_RIGHT, 12));
            right.setFocusTraversable(false);
            right.setOnAction(e -> scrollBy(tabWidth));
            left.setVisible(false);
            right.setVisible(false);
            getChildren().addAll(viewport, left, right, addButton);
            setOnScroll(e -> {
                double d = Math.abs(e.getDeltaX()) > Math.abs(e.getDeltaY()) ? e.getDeltaX() : e.getDeltaY();
                if (d != 0) {
                    scrollBy(-d);
                    e.consume();
                }
            });
        }

        void scrollBy(double delta) {
            offset += delta;
            requestLayout();
        }

        @Override
        protected double computePrefHeight(double width) {
            return HEIGHT;
        }

        @Override
        protected double computePrefWidth(double height) {
            return 200;
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            int n = tabs.size();
            boolean hasAdd = addButton.isVisible();
            double add = hasAdd ? ADD_WIDTH : 0;
            double avail = w - 2 * PAD - add;
            boolean overflow = n > 0 && avail / n < MIN_TAB;
            double vx;
            double vw;
            if (overflow) {
                tabWidth = MIN_TAB;
                vx = PAD + ARROW_WIDTH;
                vw = Math.max(0, w - 2 * PAD - add - 2 * ARROW_WIDTH);
                double max = Math.max(0, n * tabWidth - vw);
                if (reveal) {
                    Tab sel = getSelectedTab();
                    int i = tabs.indexOf(sel);
                    if (i >= 0) {
                        if (i * tabWidth < offset) {
                            offset = i * tabWidth;
                        } else if ((i + 1) * tabWidth > offset + vw) {
                            offset = (i + 1) * tabWidth - vw;
                        }
                    }
                }
                offset = Math.max(0, Math.min(max, offset));
                left.setVisible(true);
                right.setVisible(true);
                left.setDisable(offset <= 0.5);
                right.setDisable(offset >= max - 0.5);
                left.resizeRelocate(PAD, (h - 28) / 2, ARROW_WIDTH, 28);
                right.resizeRelocate(vx + vw, (h - 28) / 2, ARROW_WIDTH, 28);
            } else {
                tabWidth = n == 0 ? MIN_TAB : Math.min(MAX_TAB, avail / n);
                vx = PAD;
                vw = n * tabWidth;
                offset = 0;
                left.setVisible(false);
                right.setVisible(false);
            }
            reveal = false;
            viewport.resizeRelocate(vx, 0, vw, h);
            clip.setWidth(vw);
            clip.setHeight(h);
            for (int i = 0; i < n; i++) {
                Tab tab = tabs.get(i);
                TabHeader header = headers.get(tab);
                double x = i * tabWidth - offset;
                if (header == dragging) {
                    x = Math.max(0, Math.min(vw - tabWidth, dragging.dragLeft));
                }
                header.resizeRelocate(x, 4, Math.max(0, tabWidth - 2), h - 4);
            }
            if (hasAdd) {
                double x = overflow ? w - PAD - ADD_WIDTH : vx + vw + 2;
                addButton.resizeRelocate(x, (h - 28) / 2 + 2, 30, 28);
            }
        }
    }
}
