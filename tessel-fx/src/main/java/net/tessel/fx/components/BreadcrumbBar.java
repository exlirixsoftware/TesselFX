package net.tessel.fx.components;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * A path of clickable items separated by chevrons; the last item is the current location (bold, not clickable).
 *
 * <pre>{@code
 * BreadcrumbBar bar = new BreadcrumbBar("Home", "Documents", "Reports");
 * bar.setTruncateOnClick(true);                                   // clicking an item makes it the last one
 * bar.addItemClickListener((b, index, item) -> open(index));
 * }</pre>
 *
 * Items are any objects ({@link #setItemTextProvider} names them). When the bar is too narrow the first items collapse
 * into a "..." button that lists them in a menu. Keyboard: Left / Right / Home / End move between items, Enter or Space
 * activate, Down opens the "..." menu.
 */
public class BreadcrumbBar extends Region {

    /** Receives clicks on the items. */
    @FunctionalInterface
    public interface ItemClickListener {
        void itemClicked(BreadcrumbBar bar, int index, Object item);
    }

    private static final double HEIGHT = 32;

    private final ObservableList<Object> items = FXCollections.observableArrayList();
    private final BooleanProperty truncateOnClick = new SimpleBooleanProperty(false);
    private final List<ItemClickListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Button> crumbs = new ArrayList<>();
    private final List<Node> separators = new ArrayList<>();
    private final Button ellipsis = new Button();
    private final Node ellipsisSeparator = separator();
    private Function<Object, String> textProvider = String::valueOf;
    private Function<Object, Node> iconProvider = o -> null;
    private int hidden;

    public BreadcrumbBar(Object... items) {
        getStyleClass().add("tsl-breadcrumb");
        ellipsis.setGraphic(new Icon(Symbol.MORE, 16));
        ellipsis.getStyleClass().addAll("tsl-crumb", "tsl-crumb-more");
        ellipsis.setOnAction(e -> showHiddenMenu());
        ellipsis.setAccessibleText("More");
        this.items.addListener((ListChangeListener<Object>) c -> rebuild());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT) {
                moveFocus(-1);
                e.consume();
            } else if (e.getCode() == KeyCode.RIGHT) {
                moveFocus(1);
                e.consume();
            } else if (e.getCode() == KeyCode.HOME) {
                moveFocus(Integer.MIN_VALUE);
                e.consume();
            } else if (e.getCode() == KeyCode.END) {
                moveFocus(Integer.MAX_VALUE);
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN && ellipsis.isFocused()) {
                showHiddenMenu();
                e.consume();
            }
        });
        this.items.setAll(items);
        rebuild();
    }

    private static Node separator() {
        Icon chevron = new Icon(Symbol.CHEVRON_RIGHT, 12);
        chevron.getStyleClass().add("tsl-crumb-separator");
        return chevron;
    }

    private void rebuild() {
        getChildren().clear();
        crumbs.clear();
        separators.clear();
        getChildren().addAll(ellipsis, ellipsisSeparator);
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            boolean last = i == items.size() - 1;
            Button b = new Button(textProvider.apply(item));
            b.setMnemonicParsing(false);
            b.getStyleClass().add("tsl-crumb");
            Node icon = iconProvider.apply(item);
            if (icon != null) {
                b.setGraphic(icon);
            }
            if (last) {
                b.getStyleClass().addAll("tsl-crumb-current", "tsl-semibold");
                b.setFocusTraversable(false);
                b.setMouseTransparent(true);
            } else {
                final int index = i;
                b.setOnAction(e -> click(index));
            }
            crumbs.add(b);
            getChildren().add(b);
            if (!last) {
                Node s = separator();
                separators.add(s);
                getChildren().add(s);
            }
        }
        requestLayout();
    }

    private void click(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        Object item = items.get(index);
        if (isTruncateOnClick() && index < items.size() - 1) {
            items.remove(index + 1, items.size());
        }
        for (ItemClickListener l : listeners) {
            l.itemClicked(this, index, item);
        }
    }

    private void showHiddenMenu() {
        ContextMenu menu = new ContextMenu();
        for (int i = 0; i < hidden && i < items.size() - 1; i++) {
            final int index = i;
            MenuItem mi = new MenuItem(textProvider.apply(items.get(i)));
            mi.setMnemonicParsing(false);
            Node icon = iconProvider.apply(items.get(i));
            if (icon != null) {
                mi.setGraphic(icon);
            }
            mi.setOnAction(e -> click(index));
            menu.getItems().add(mi);
        }
        menu.show(ellipsis, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void moveFocus(int delta) {
        List<Node> focusable = new ArrayList<>();
        if (hidden > 0) {
            focusable.add(ellipsis);
        }
        for (int i = hidden; i < crumbs.size() - 1; i++) {
            focusable.add(crumbs.get(i));
        }
        if (focusable.isEmpty()) {
            return;
        }
        int at = -1;
        for (int i = 0; i < focusable.size(); i++) {
            if (focusable.get(i).isFocused()) {
                at = i;
            }
        }
        int target;
        if (delta == Integer.MIN_VALUE) {
            target = 0;
        } else if (delta == Integer.MAX_VALUE) {
            target = focusable.size() - 1;
        } else {
            target = Math.max(0, Math.min(focusable.size() - 1, at < 0 ? 0 : at + delta));
        }
        focusable.get(target).requestFocus();
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected double computePrefWidth(double height) {
        double w = 0;
        for (int i = 0; i < crumbs.size(); i++) {
            w += crumbs.get(i).prefWidth(-1);
            if (i < separators.size()) {
                w += separators.get(i).prefWidth(-1) + 4;
            }
        }
        return snapSpaceX(getInsets().getLeft()) + w + snapSpaceX(getInsets().getRight());
    }

    @Override
    protected double computeMinWidth(double height) {
        double w = 0;
        if (crumbs.size() > 1) {
            w += ellipsis.prefWidth(-1) + ellipsisSeparator.prefWidth(-1) + 4;
        }
        if (!crumbs.isEmpty()) {
            w += crumbs.get(crumbs.size() - 1).prefWidth(-1);
        }
        return snapSpaceX(getInsets().getLeft()) + w + snapSpaceX(getInsets().getRight());
    }

    @Override
    protected double computePrefHeight(double width) {
        return HEIGHT;
    }

    @Override
    protected void layoutChildren() {
        double available = getWidth() - getInsets().getLeft() - getInsets().getRight();
        double sepW = 4 + 12;
        int n = crumbs.size();
        double[] widths = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            widths[i] = crumbs.get(i).prefWidth(-1);
            total += widths[i] + (i < n - 1 ? sepW : 0);
        }
        double ellW = ellipsis.prefWidth(-1) + sepW;
        hidden = 0;
        double shown = total;
        while (shown > available && hidden < n - 1) {
            shown -= widths[hidden] + sepW;
            hidden++;
            if (hidden == 1) {
                shown += ellW;
            }
        }
        double x = getInsets().getLeft();
        double h = getHeight();
        boolean showEllipsis = hidden > 0;
        ellipsis.setVisible(showEllipsis);
        ellipsisSeparator.setVisible(showEllipsis);
        if (showEllipsis) {
            double ew = ellipsis.prefWidth(-1);
            ellipsis.resizeRelocate(x, (h - HEIGHT) / 2, ew, HEIGHT);
            x += ew + 2;
            ellipsisSeparator.resizeRelocate(x, (h - 12) / 2, 12, 12);
            x += 12 + 2;
        }
        for (int i = 0; i < n; i++) {
            Button b = crumbs.get(i);
            boolean visible = i >= hidden;
            b.setVisible(visible);
            if (i < separators.size()) {
                separators.get(i).setVisible(visible);
            }
            if (!visible) {
                continue;
            }
            double w = Math.min(widths[i], Math.max(40, available - x));
            b.resizeRelocate(x, (h - HEIGHT) / 2, w, HEIGHT);
            x += w + 2;
            if (i < separators.size()) {
                separators.get(i).resizeRelocate(x, (h - 12) / 2, 12, 12);
                x += 12 + 2;
            }
        }
    }

    // ------------------------------------------------------------------ properties

    /** The items of the path; edit this list to change the bar. */
    public final ObservableList<Object> getItems() {
        return items;
    }

    /** True to remove the items after a clicked one, so that it becomes the current location. */
    public final boolean isTruncateOnClick() {
        return truncateOnClick.get();
    }

    public final void setTruncateOnClick(boolean truncate) {
        truncateOnClick.set(truncate);
    }

    public final BooleanProperty truncateOnClickProperty() {
        return truncateOnClick;
    }

    /** How items are named; the default is {@code String.valueOf}. */
    public final void setItemTextProvider(Function<Object, String> provider) {
        this.textProvider = provider == null ? String::valueOf : provider;
        rebuild();
    }

    /** An icon for an item, or null. */
    public final void setItemIconProvider(Function<Object, Node> provider) {
        this.iconProvider = provider == null ? o -> null : provider;
        rebuild();
    }

    public final void addItemClickListener(ItemClickListener listener) {
        listeners.add(listener);
    }

    public final void removeItemClickListener(ItemClickListener listener) {
        listeners.remove(listener);
    }
}
