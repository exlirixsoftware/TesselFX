package net.tessel.fx.icons;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import net.tessel.fx.Palette;

/**
 * A vector glyph from the Tessel icon set. By default it is drawn in the text color of the control that contains it
 * (for example white on an accent button); call {@link #setColor(Color)} for a fixed color.
 *
 * <pre>{@code
 * button.setGraphic(new Icon(Symbol.SAVE));
 * button.setGraphic(new Icon(Symbol.SETTINGS, 20));
 * }</pre>
 */
public class Icon extends Region {

    /** The nominal size of the glyph table (the SVG view box). */
    private static final double VIEW_BOX = 20;

    private final ObjectProperty<String> key = new SimpleObjectProperty<>();
    private final DoubleProperty size = new SimpleDoubleProperty(16);
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private final Group glyph = new Group();
    private final Scale scale = new Scale(1, 1, 0, 0);

    public Icon(Symbol symbol) {
        this(symbol, 16);
    }

    public Icon(Symbol symbol, double size) {
        this(symbol.key(), size);
    }

    public Icon(Symbol symbol, double size, Color color) {
        this(symbol.key(), size);
        setColor(color);
    }

    /** @param key a glyph key such as {@code "home"} or {@code "sev-success"}; see {@link Symbol#key()} */
    public Icon(String key, double size) {
        getStyleClass().add("tsl-icon");
        setMouseTransparent(true);
        setSnapToPixel(false);
        glyph.getTransforms().add(scale);
        getChildren().add(glyph);

        this.size.addListener((o, a, b) -> resize());
        this.key.addListener((o, a, b) -> rebuild());
        color.addListener((o, a, b) -> setStyle(b == null ? "" : "-tsl-icon-color: " + Palette.css(b) + ";"));

        this.size.set(size);
        this.key.set(key);
        resize();
        rebuild();
    }

    private void rebuild() {
        glyph.getChildren().clear();
        Rectangle box = new Rectangle(0, 0, VIEW_BOX, VIEW_BOX);
        box.setFill(Color.TRANSPARENT);
        glyph.getChildren().add(box);
        for (Node n : IconSvg.build(key.get())) {
            glyph.getChildren().add(n);
        }
    }

    private void resize() {
        double s = size.get();
        double f = s / VIEW_BOX;
        scale.setX(f);
        scale.setY(f);
        setMinSize(s, s);
        setPrefSize(s, s);
        setMaxSize(s, s);
    }

    public final String getKey() {
        return key.get();
    }

    public final void setSymbol(Symbol symbol) {
        key.set(symbol.key());
    }

    public final void setKey(String key) {
        this.key.set(key);
    }

    public final ObjectProperty<String> keyProperty() {
        return key;
    }

    public final double getSize() {
        return size.get();
    }

    public final void setSize(double size) {
        this.size.set(size);
    }

    public final DoubleProperty sizeProperty() {
        return size;
    }

    /** The fixed color, or null to follow the surrounding text color. */
    public final Color getColor() {
        return color.get();
    }

    public final void setColor(Color color) {
        this.color.set(color);
    }

    public final ObjectProperty<Color> colorProperty() {
        return color;
    }

    @Override
    protected void layoutChildren() {
        // the glyph group sits at the origin and is scaled; nothing to lay out
    }
}
