package net.tessel.fx.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * The main navigation of an app: a pane of items on the left (collapsible to an icon rail) and the selected page next
 * to it. Pages are created lazily, the first time their item is selected, and cached.
 *
 * <pre>{@code
 * NavigationView nav = new NavigationView("My App");
 * nav.setPaneHeader(searchField);
 * nav.addItem("Home", Symbol.HOME, HomePage::new);
 * nav.addHeader("Section");
 * nav.addFooterItem("Settings", Symbol.SETTINGS, SettingsPage::new);
 * nav.select(0);
 * }</pre>
 *
 * The pane collapses to a rail by itself when the view gets narrower than {@link #autoCompactWidthProperty()}, and the
 * button in the top bar toggles it.
 */
public class NavigationView extends BorderPane {

    /** A navigation entry. */
    public static final class Item {
        private final StringProperty title = new SimpleStringProperty();
        private final ObjectProperty<Symbol> symbol = new SimpleObjectProperty<>();
        private final BooleanProperty disabled = new SimpleBooleanProperty();
        private final Supplier<? extends Node> factory;
        private final boolean footer;
        private Node page;
        private Object userData;
        private HBox node;

        private Item(String title, Symbol symbol, Supplier<? extends Node> factory, boolean footer) {
            this.title.set(title);
            this.symbol.set(symbol);
            this.factory = factory;
            this.footer = footer;
        }

        public String getTitle() {
            return title.get();
        }

        public void setTitle(String title) {
            this.title.set(title);
        }

        public StringProperty titleProperty() {
            return title;
        }

        public Symbol getSymbol() {
            return symbol.get();
        }

        public void setSymbol(Symbol symbol) {
            this.symbol.set(symbol);
        }

        public ObjectProperty<Symbol> symbolProperty() {
            return symbol;
        }

        public boolean isDisabled() {
            return disabled.get();
        }

        public void setDisabled(boolean disabled) {
            this.disabled.set(disabled);
        }

        public BooleanProperty disabledProperty() {
            return disabled;
        }

        public boolean isFooter() {
            return footer;
        }

        public Object getUserData() {
            return userData;
        }

        public void setUserData(Object userData) {
            this.userData = userData;
        }

        /** The page of this item, or null when it has not been created yet. */
        public Node getPage() {
            return page;
        }

        Node page() {
            if (page == null && factory != null) {
                page = factory.get();
            }
            return page;
        }
    }

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");
    private static final double PANE_OPEN = 260;
    private static final double PANE_COMPACT = 56;

    private final StringProperty appTitle = new SimpleStringProperty("");
    private final BooleanProperty paneOpen = new SimpleBooleanProperty(true);
    private final BooleanProperty paneToggleVisible = new SimpleBooleanProperty(true);
    private final DoubleProperty autoCompactWidth = new SimpleDoubleProperty(720);
    private final ObjectProperty<Item> selectedItem = new SimpleObjectProperty<>();
    private final ObjectProperty<Node> paneHeader = new SimpleObjectProperty<>();
    private final DoubleProperty paneWidth = new SimpleDoubleProperty(PANE_OPEN);

    private final List<Item> items = new ArrayList<>();
    private final VBox itemsBox = new VBox(2);
    private final VBox footerBox = new VBox(2);
    private final StackPane headerHost = new StackPane();
    private final HBox topActions = new HBox(4);
    private final StackPane contentHost = new StackPane();
    private final Pane indicatorLayer = new Pane();
    private final Region indicator = new Region();
    private final ScrollPane scroll = new ScrollPane(itemsBox);
    private final InvalidationListener indicatorFollow = o -> placeIndicator();
    private Item tracked;
    private Timeline widthAnimation;
    private Timeline indicatorAnimation;
    private boolean wasNarrow;

    public NavigationView(String title) {
        getStyleClass().add("tsl-nav-view");
        setAppTitle(title);

        // top bar
        Button toggle = Tessel.style(new Button(), ButtonStyle.ICON);
        toggle.setGraphic(new Icon(Symbol.GLOBAL_NAV, 16));
        toggle.getStyleClass().add("tsl-nav-toggle");
        toggle.setOnAction(e -> setPaneOpen(!isPaneOpen()));
        toggle.managedProperty().bind(toggle.visibleProperty());
        toggle.visibleProperty().bind(paneToggleVisible);
        toggle.setAccessibleText("Toggle navigation");
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(appTitle);
        titleLabel.getStyleClass().addAll("tsl-nav-title", "tsl-semibold");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        topActions.setAlignment(Pos.CENTER_RIGHT);
        HBox topBar = new HBox(8, toggle, titleLabel, topActions);
        topBar.getStyleClass().add("tsl-nav-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        setTop(topBar);

        // pane
        headerHost.getStyleClass().add("tsl-nav-pane-header");
        headerHost.managedProperty().bind(headerHost.visibleProperty());
        headerHost.setVisible(false);
        itemsBox.getStyleClass().add("tsl-nav-items");
        footerBox.getStyleClass().add("tsl-nav-footer");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("tsl-nav-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox paneRoot = new VBox(headerHost, scroll, footerBox);
        paneRoot.getStyleClass().add("tsl-nav-pane");

        indicator.getStyleClass().add("tsl-nav-indicator");
        indicator.setMinSize(3, 16);
        indicator.setPrefSize(3, 16);
        indicator.setMaxSize(3, 16);
        indicator.setVisible(false);
        indicator.setManaged(false);
        indicatorLayer.getChildren().add(indicator);
        indicatorLayer.setMouseTransparent(true);
        indicatorLayer.setPickOnBounds(false);
        indicatorLayer.setManaged(false);

        StackPane paneStack = new StackPane(paneRoot, indicatorLayer) {
            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                indicatorLayer.resizeRelocate(0, 0, getWidth(), getHeight());
            }
        };
        paneStack.setMinWidth(PANE_COMPACT);
        paneStack.prefWidthProperty().bind(paneWidth);
        paneStack.maxWidthProperty().bind(paneWidth);
        paneStack.minWidthProperty().bind(paneWidth);
        Rectangle paneClip = new Rectangle();
        paneClip.widthProperty().bind(paneStack.widthProperty());
        paneClip.heightProperty().bind(paneStack.heightProperty());
        paneStack.setClip(paneClip);
        setLeft(paneStack);

        contentHost.getStyleClass().add("tsl-nav-content");
        setCenter(contentHost);

        paneOpen.addListener((o, a, b) -> applyPaneState(true));
        paneHeader.addListener((o, a, b) -> {
            headerHost.getChildren().clear();
            headerHost.setVisible(b != null && isPaneOpen());
            if (b != null) {
                headerHost.getChildren().add(b);
            }
        });
        selectedItem.addListener((o, a, b) -> onSelected(a, b));
        widthProperty().addListener((o, a, b) -> {
            double limit = getAutoCompactWidth();
            boolean narrow = limit > 0 && b.doubleValue() > 0 && b.doubleValue() < limit;
            if (narrow != wasNarrow) {
                wasNarrow = narrow;
                setPaneOpen(!narrow);
            }
        });
        scroll.vvalueProperty().addListener(indicatorFollow);
        sceneProperty().addListener((o, a, b) -> javafx.application.Platform.runLater(this::placeIndicator));
        applyPaneState(false);
    }

    // ------------------------------------------------------------------ items

    /** Adds an item to the main list. The page is created the first time the item is selected. */
    public Item addItem(String title, Symbol symbol, Supplier<? extends Node> page) {
        return add(new Item(title, symbol, page, false), itemsBox);
    }

    /** Adds an item to the footer, below the scrolling list (settings, account...). */
    public Item addFooterItem(String title, Symbol symbol, Supplier<? extends Node> page) {
        return add(new Item(title, symbol, page, true), footerBox);
    }

    /** Adds a small caption between items. */
    public void addHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("tsl-nav-header-label", "tsl-semibold");
        Region line = new Region();
        line.getStyleClass().add("tsl-nav-header-line");
        StackPane header = new StackPane(label, line);
        header.getStyleClass().add("tsl-nav-header");
        StackPane.setAlignment(label, Pos.CENTER_LEFT);
        label.setMaxWidth(Double.MAX_VALUE);
        // the caption turns into a thin divider in the compact rail
        label.visibleProperty().bind(paneOpen);
        line.visibleProperty().bind(paneOpen.not());
        itemsBox.getChildren().add(header);
    }

    private Item add(Item item, VBox box) {
        HBox row = new HBox();
        row.getStyleClass().add("tsl-nav-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setFocusTraversable(true);
        StackPane iconHost = new StackPane();
        iconHost.getStyleClass().add("tsl-nav-item-icon");
        item.symbol.addListener((o, a, b) -> setIcon(iconHost, b));
        setIcon(iconHost, item.getSymbol());
        Label label = new Label();
        label.textProperty().bind(item.title);
        label.getStyleClass().add("tsl-nav-item-label");
        label.setMaxWidth(Double.MAX_VALUE);
        label.managedProperty().bind(label.visibleProperty());
        label.visibleProperty().bind(paneOpen);
        HBox.setHgrow(label, Priority.ALWAYS);
        row.getChildren().addAll(iconHost, label);
        row.disableProperty().bind(item.disabled);
        Tooltip tip = new Tooltip();
        tip.textProperty().bind(item.title);

        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                select(item);
                row.requestFocus();
            }
        });
        row.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                select(item);
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.UP) {
                focusSibling(item, e.getCode() == KeyCode.DOWN ? 1 : -1);
                e.consume();
            }
        });
        paneOpen.addListener((o, a, b) -> {
            if (b) {
                Tooltip.uninstall(row, tip);
            } else {
                Tooltip.install(row, tip);
            }
        });
        if (!isPaneOpen()) {
            Tooltip.install(row, tip);
        }
        item.node = row;
        items.add(item);
        box.getChildren().add(row);
        return item;
    }

    private void setIcon(StackPane host, Symbol symbol) {
        host.getChildren().clear();
        if (symbol != null) {
            host.getChildren().add(new Icon(symbol, 18));
        }
    }

    private void focusSibling(Item from, int delta) {
        int at = items.indexOf(from);
        for (int i = at + delta; i >= 0 && i < items.size(); i += delta) {
            Item other = items.get(i);
            if (!other.isDisabled()) {
                other.node.requestFocus();
                return;
            }
        }
    }

    /** Selects the item at this position among the items (headers do not count). */
    public void select(int index) {
        if (index >= 0 && index < items.size()) {
            select(items.get(index));
        }
    }

    public void select(Item item) {
        if (item != null && !item.isDisabled()) {
            selectedItem.set(item);
        }
    }

    private void onSelected(Item old, Item now) {
        if (old != null && old.node != null) {
            old.node.pseudoClassStateChanged(SELECTED, false);
        }
        if (now == null) {
            contentHost.getChildren().clear();
            indicator.setVisible(false);
            return;
        }
        now.node.pseudoClassStateChanged(SELECTED, true);
        Node page = now.page();
        contentHost.getChildren().setAll(page == null ? new Region() : page);
        if (page != null && old != null) {
            page.setOpacity(0);
            page.setTranslateY(16);
            FadeTransition fade = new FadeTransition(Duration.millis(220), page);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), page);
            slide.setToY(0);
            slide.setInterpolator(Collapsible.EASE);
            new ParallelTransition(fade, slide).play();
        }
        // the indicator follows the selected row, sliding from the previous one
        if (tracked != null && tracked.node != null) {
            tracked.node.localToSceneTransformProperty().removeListener(indicatorFollow);
            tracked.node.layoutBoundsProperty().removeListener(indicatorFollow);
        }
        tracked = now;
        now.node.localToSceneTransformProperty().addListener(indicatorFollow);
        now.node.layoutBoundsProperty().addListener(indicatorFollow);
        boolean wasVisible = indicator.isVisible();
        double from = indicator.getLayoutY() + indicator.getTranslateY();
        indicatorLayer.applyCss();
        placeIndicator();
        if (wasVisible && old != null) {
            if (indicatorAnimation != null) {
                indicatorAnimation.stop();
            }
            indicator.setTranslateY(from - indicator.getLayoutY());
            indicatorAnimation = new Timeline(new KeyFrame(Duration.millis(220),
                    new KeyValue(indicator.translateYProperty(), 0, Collapsible.EASE)));
            indicatorAnimation.play();
        }
        javafx.application.Platform.runLater(this::placeIndicator);
    }

    private void placeIndicator() {
        Item item = tracked;
        if (item == null || item.node == null) {
            return;
        }
        // invalidation listeners are only called again after the value was read, so read it to stay subscribed
        item.node.getLocalToSceneTransform();
        item.node.getLayoutBounds();
        if (item.node.getScene() == null || indicatorLayer.getScene() == null) {
            return;
        }
        Bounds b = indicatorLayer.sceneToLocal(item.node.localToScene(item.node.getBoundsInLocal()));
        if (b == null) {
            return;
        }
        // the indicator is unmanaged, so nothing but this call gives it a size
        indicator.resizeRelocate(b.getMinX(), b.getMinY() + (b.getHeight() - 16) / 2, 3, 16);
        indicator.setVisible(true);
    }

    private void applyPaneState(boolean animate) {
        boolean open = isPaneOpen();
        pseudoClassStateChanged(COMPACT, !open);
        headerHost.setVisible(paneHeader.get() != null && open);
        double target = open ? PANE_OPEN : PANE_COMPACT;
        if (widthAnimation != null) {
            widthAnimation.stop();
        }
        if (!animate) {
            paneWidth.set(target);
            return;
        }
        widthAnimation = new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(paneWidth, target, Collapsible.EASE)));
        widthAnimation.setOnFinished(e -> placeIndicator());
        widthAnimation.play();
    }

    // ------------------------------------------------------------------ properties

    /** All items (not the headers), in the order they were added. */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public final Item getSelectedItem() {
        return selectedItem.get();
    }

    public final ObjectProperty<Item> selectedItemProperty() {
        return selectedItem;
    }

    public final String getAppTitle() {
        return appTitle.get();
    }

    public final void setAppTitle(String title) {
        appTitle.set(title == null ? "" : title);
    }

    public final StringProperty appTitleProperty() {
        return appTitle;
    }

    /** True while the pane is expanded; false while it is the icon rail. */
    public final boolean isPaneOpen() {
        return paneOpen.get();
    }

    public final void setPaneOpen(boolean open) {
        paneOpen.set(open);
    }

    public final BooleanProperty paneOpenProperty() {
        return paneOpen;
    }

    public final boolean isPaneToggleVisible() {
        return paneToggleVisible.get();
    }

    public final void setPaneToggleVisible(boolean visible) {
        paneToggleVisible.set(visible);
    }

    public final BooleanProperty paneToggleVisibleProperty() {
        return paneToggleVisible;
    }

    /** The width below which the pane collapses to the rail by itself (0 turns this off); default 720. */
    public final double getAutoCompactWidth() {
        return autoCompactWidth.get();
    }

    public final void setAutoCompactWidth(double width) {
        autoCompactWidth.set(width);
    }

    public final DoubleProperty autoCompactWidthProperty() {
        return autoCompactWidth;
    }

    /** A node above the item list, such as a search box. It is hidden in the compact rail. */
    public final Node getPaneHeader() {
        return paneHeader.get();
    }

    public final void setPaneHeader(Node header) {
        paneHeader.set(header);
    }

    public final ObjectProperty<Node> paneHeaderProperty() {
        return paneHeader;
    }

    /** The components at the right end of the top bar. */
    public final ObservableList<Node> getTopBarComponents() {
        return topActions.getChildren();
    }

    /** Adds a component at the right end of the top bar. */
    public void addTopBarComponent(Node node) {
        topActions.getChildren().add(node);
    }

    /** The area the pages are shown in. */
    public final StackPane getContentArea() {
        return contentHost;
    }
}
