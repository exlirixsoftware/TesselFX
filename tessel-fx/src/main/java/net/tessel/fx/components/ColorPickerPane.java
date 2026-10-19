package net.tessel.fx.components;

import java.util.Locale;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import net.tessel.fx.Palette;

/**
 * A color picker panel: a saturation / brightness square, a hue slider, an optional opacity slider, a hex field,
 * RGB(A) number boxes and a row of swatches. Use it on its own or inside a pop-over or {@link ContentDialog}.
 *
 * <pre>{@code
 * ColorPickerPane picker = new ColorPickerPane(Color.ORANGE);
 * picker.setAlphaEnabled(true);                       // adds the opacity slider and field
 * picker.colorProperty().addListener((o, was, is) -> use(is));
 * }</pre>
 */
public class ColorPickerPane extends VBox {

    private static final String[] SWATCHES = {
            "#EF4444", "#F97316", "#F59E0B", "#84CC16", "#22C55E", "#14B8A6",
            "#06B6D4", "#3B82F6", "#4F46E5", "#8B5CF6", "#EC4899", "#64748B",
            "#000000", "#FFFFFF"
    };

    private static final WritableImage CHECKER = checker();

    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#4F46E5"));
    private final BooleanProperty alphaEnabled = new SimpleBooleanProperty(false);

    private double hue;
    private double saturation;
    private double brightness;
    private double alpha = 1;
    private boolean updating;

    private final Square square = new Square();
    private final Bar hueBar = new Bar(false);
    private final Bar alphaBar = new Bar(true);
    private final Region preview = new Region();
    private final TextField hex = new TextField();
    private final NumberBox red = channelBox(255);
    private final NumberBox green = channelBox(255);
    private final NumberBox blue = channelBox(255);
    private final NumberBox opacity = channelBox(100);
    private final VBox opacityColumn;

    public ColorPickerPane() {
        this(Color.web("#4F46E5"));
    }

    public ColorPickerPane(Color initial) {
        getStyleClass().add("tsl-color-picker");
        setSpacing(12);
        setPrefWidth(300);
        setPadding(new Insets(0));

        square.onChange = this::fromSquare;
        hueBar.onChange = this::fromHue;
        alphaBar.onChange = this::fromAlpha;
        hueBar.setHueGradient();
        alphaBar.managedProperty().bind(alphaBar.visibleProperty());
        alphaBar.visibleProperty().bind(alphaEnabled);

        preview.getStyleClass().add("tsl-color-preview");
        preview.setMinSize(36, 36);
        preview.setPrefSize(36, 36);
        preview.setMaxSize(36, 36);

        hex.getStyleClass().add("tsl-color-hex");
        hex.setPrefColumnCount(8);
        hex.setOnAction(e -> commitHex());
        hex.focusedProperty().addListener((o, a, b) -> {
            if (!b) {
                commitHex();
            }
        });
        HBox.setHgrow(hex, Priority.ALWAYS);

        HBox top = new HBox(10, preview, hex);
        top.setAlignment(Pos.CENTER_LEFT);

        opacityColumn = labeled("A", opacity);
        opacityColumn.managedProperty().bind(opacityColumn.visibleProperty());
        opacityColumn.visibleProperty().bind(alphaEnabled);
        opacity.setSuffix("%");
        HBox channels = new HBox(8, labeled("R", red), labeled("G", green), labeled("B", blue), opacityColumn);
        for (javafx.scene.Node n : channels.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        FlowPane swatches = new FlowPane(8, 8);
        swatches.getStyleClass().add("tsl-color-swatches");
        for (String code : SWATCHES) {
            Button b = new Button();
            b.getStyleClass().add("tsl-color-swatch");
            b.setStyle("-fx-background-color: " + code + ";");
            b.setFocusTraversable(true);
            b.setOnAction(e -> {
                Color c = Color.web(code);
                setColor(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alphaEnabled.get() ? alpha : 1));
            });
            swatches.getChildren().add(b);
        }

        getChildren().addAll(square, hueBar, alphaBar, top, channels, swatches);

        red.valueProperty().addListener((o, a, b) -> fromChannels());
        green.valueProperty().addListener((o, a, b) -> fromChannels());
        blue.valueProperty().addListener((o, a, b) -> fromChannels());
        opacity.valueProperty().addListener((o, a, b) -> fromChannels());
        alphaEnabled.addListener((o, a, b) -> {
            if (!b && alpha != 1) {
                alpha = 1;
                publish();
            }
            refresh();
        });
        color.addListener((o, a, b) -> {
            if (!updating && b != null) {
                load(b);
                refresh();
            }
        });
        load(initial == null ? Color.web("#4F46E5") : initial);
        color.set(initial == null ? Color.web("#4F46E5") : initial);
        refresh();
    }

    private VBox labeled(String caption, NumberBox box) {
        Label l = new Label(caption);
        l.getStyleClass().add("tsl-color-caption");
        box.setSpinButtonPlacement(NumberBox.SpinButtonPlacement.HIDDEN);
        box.setPrefWidth(10);
        box.setMaxWidth(Double.MAX_VALUE);
        VBox column = new VBox(4, l, box);
        return column;
    }

    private static NumberBox channelBox(int max) {
        NumberBox b = new NumberBox(0, 0, max);
        b.setFractionDigits(0);
        return b;
    }

    // ------------------------------------------------------------------ model <-> view

    private void load(Color c) {
        double s = c.getSaturation();
        double v = c.getBrightness();
        if (s > 0 && v > 0) {
            hue = c.getHue();
        }
        saturation = s;
        brightness = v;
        alpha = alphaEnabled.get() ? c.getOpacity() : 1;
    }

    private void publish() {
        updating = true;
        try {
            color.set(Color.hsb(hue, saturation, brightness, alpha));
        } finally {
            updating = false;
        }
    }

    private void fromSquare() {
        saturation = square.s;
        brightness = square.v;
        publish();
        refresh();
    }

    private void fromHue() {
        hue = hueBar.value * 360;
        publish();
        refresh();
    }

    private void fromAlpha() {
        alpha = alphaBar.value;
        publish();
        refresh();
    }

    private void fromChannels() {
        if (updating || channelsLocked) {
            return;
        }
        if (Double.isNaN(red.getValue()) || Double.isNaN(green.getValue()) || Double.isNaN(blue.getValue())) {
            return;
        }
        Color c = Color.rgb((int) red.getValue(), (int) green.getValue(), (int) blue.getValue(),
                alphaEnabled.get() ? opacity.getValue() / 100 : 1);
        load(c);
        publish();
        refresh();
    }

    private boolean channelsLocked;

    private void commitHex() {
        String t = hex.getText().trim().replace("#", "");
        try {
            if (t.length() == 3) {
                t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
            }
            Color c;
            if (t.length() == 6) {
                c = Color.web("#" + t);
            } else if (t.length() == 8) {
                c = Color.web("#" + t);
            } else {
                throw new IllegalArgumentException(t);
            }
            load(c);
            publish();
        } catch (IllegalArgumentException ex) {
            // keep the old color
        }
        refresh();
    }

    /** Pushes the model to every control. */
    private void refresh() {
        Color c = Color.hsb(hue, saturation, brightness, alpha);
        square.hue = hue;
        square.s = saturation;
        square.v = brightness;
        square.update();
        hueBar.value = hue / 360;
        hueBar.update(null);
        alphaBar.value = alpha;
        alphaBar.update(Color.hsb(hue, saturation, brightness, 1));
        Color solid = Color.hsb(hue, saturation, brightness, 1);
        String solidCss = Palette.css(solid);
        square.thumb.setStyle("-fx-background-color: rgba(0,0,0,0.35), white, " + solidCss
                + "; -fx-background-insets: -1, 0, 3; -fx-background-radius: 10, 9, 6;");
        hueBar.thumb.setStyle("-fx-background-color: rgba(0,0,0,0.35), white, "
                + Palette.css(Color.hsb(hue, 1, 1)) + "; -fx-background-insets: -1, 0, 3; -fx-background-radius: 10, 9, 6;");
        alphaBar.thumb.setStyle("-fx-background-color: rgba(0,0,0,0.35), white; -fx-background-insets: -1, 0;"
                + " -fx-background-radius: 10, 9;");
        preview.setBackground(new Background(
                new BackgroundFill(new ImagePattern(CHECKER, 0, 0, 8, 8, false), new CornerRadii(6), null),
                new BackgroundFill(c, new CornerRadii(6), null)));

        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        int a = (int) Math.round(c.getOpacity() * 255);
        if (!hex.isFocused()) {
            hex.setText(alphaEnabled.get() && a != 255
                    ? String.format(Locale.ROOT, "#%02X%02X%02X%02X", r, g, b, a)
                    : String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b));
        }
        channelsLocked = true;
        try {
            red.setValue(r);
            green.setValue(g);
            blue.setValue(b);
            opacity.setValue(Math.round(alpha * 100));
        } finally {
            channelsLocked = false;
        }
    }

    // ------------------------------------------------------------------ properties

    public final Color getColor() {
        return color.get();
    }

    public final void setColor(Color color) {
        this.color.set(color);
    }

    public final ObjectProperty<Color> colorProperty() {
        return color;
    }

    /** True to show the opacity slider and field; the color then keeps its alpha channel. */
    public final boolean isAlphaEnabled() {
        return alphaEnabled.get();
    }

    public final void setAlphaEnabled(boolean enabled) {
        alphaEnabled.set(enabled);
    }

    public final BooleanProperty alphaEnabledProperty() {
        return alphaEnabled;
    }

    // ------------------------------------------------------------------ parts

    private static WritableImage checker() {
        WritableImage img = new WritableImage(8, 8);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                boolean dark = ((x / 4) + (y / 4)) % 2 == 0;
                img.getPixelWriter().setColor(x, y, dark ? Color.web("#C8C8C8") : Color.web("#F4F4F4"));
            }
        }
        return img;
    }

    /** The saturation (x) and brightness (y) square. */
    private static final class Square extends Region {
        double hue;
        double s;
        double v;
        Runnable onChange;
        final Region thumb = new Region();

        Square() {
            getStyleClass().add("tsl-color-square");
            setPrefSize(300, 170);
            setMinHeight(120);
            thumb.getStyleClass().add("tsl-color-thumb");
            thumb.setMouseTransparent(true);
            getChildren().add(thumb);
            setOnMousePressed(e -> pick(e.getX(), e.getY()));
            setOnMouseDragged(e -> pick(e.getX(), e.getY()));
        }

        private void pick(double x, double y) {
            s = Math.max(0, Math.min(1, x / getWidth()));
            v = 1 - Math.max(0, Math.min(1, y / getHeight()));
            requestLayout();
            if (onChange != null) {
                onChange.run();
            }
        }

        void update() {
            CornerRadii r = new CornerRadii(6);
            setBackground(new Background(
                    new BackgroundFill(Color.hsb(hue, 1, 1), r, null),
                    new BackgroundFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.WHITE), new Stop(1, Color.rgb(255, 255, 255, 0))), r, null),
                    new BackgroundFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(0, 0, 0, 0)), new Stop(1, Color.BLACK)), r, null)));
            requestLayout();
        }

        @Override
        protected void layoutChildren() {
            thumb.resizeRelocate(s * getWidth() - 8, (1 - v) * getHeight() - 8, 16, 16);
        }
    }

    /** A horizontal slider drawn as a gradient (hue or opacity). */
    private static final class Bar extends Region {
        final boolean alphaBar;
        double value;
        Runnable onChange;
        final Region thumb = new Region();

        Bar(boolean alphaBar) {
            this.alphaBar = alphaBar;
            getStyleClass().add("tsl-color-bar");
            setPrefHeight(14);
            setMinHeight(14);
            setMaxHeight(14);
            thumb.getStyleClass().add("tsl-color-thumb");
            thumb.setMouseTransparent(true);
            getChildren().add(thumb);
            setOnMousePressed(e -> pick(e.getX()));
            setOnMouseDragged(e -> pick(e.getX()));
        }

        void setHueGradient() {
            setBackground(new Background(new BackgroundFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.hsb(0, 1, 1)), new Stop(1.0 / 6, Color.hsb(60, 1, 1)),
                    new Stop(2.0 / 6, Color.hsb(120, 1, 1)), new Stop(3.0 / 6, Color.hsb(180, 1, 1)),
                    new Stop(4.0 / 6, Color.hsb(240, 1, 1)), new Stop(5.0 / 6, Color.hsb(300, 1, 1)),
                    new Stop(1, Color.hsb(360, 1, 1))), new CornerRadii(7), null)));
        }

        private void pick(double x) {
            value = Math.max(0, Math.min(1, x / getWidth()));
            requestLayout();
            if (onChange != null) {
                onChange.run();
            }
        }

        void update(Color solid) {
            if (alphaBar && solid != null) {
                CornerRadii r = new CornerRadii(7);
                setBackground(new Background(
                        new BackgroundFill(new ImagePattern(CHECKER, 0, 0, 8, 8, false), r, null),
                        new BackgroundFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                                new Stop(0, Color.color(solid.getRed(), solid.getGreen(), solid.getBlue(), 0)),
                                new Stop(1, solid)), r, null)));
            }
            requestLayout();
        }

        @Override
        protected void layoutChildren() {
            thumb.resizeRelocate(value * getWidth() - 9, (getHeight() - 18) / 2, 18, 18);
        }
    }
}
