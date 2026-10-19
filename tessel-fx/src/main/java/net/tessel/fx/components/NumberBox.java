package net.tessel.fx.components;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * Number input with step buttons, a range, and arithmetic: type {@code 12 * 3 + 4} and press Enter.
 *
 * <pre>{@code
 * NumberBox quantity = new NumberBox(1, 0, 99);
 * quantity.setSuffix(" pcs");
 * quantity.valueProperty().addListener((o, was, is) -> ...);
 * }</pre>
 *
 * The value is committed on Enter, on focus loss and when stepped (arrow keys, Page Up / Down, the mouse wheel and the
 * buttons). It is clamped to the range, can wrap around ({@link #setWrapEnabled}), and can be empty
 * ({@link #setNullable}, the value is then {@code NaN}). Text that cannot be evaluated flashes red and the old
 * value is restored.
 */
public class NumberBox extends HBox {

    /** Where the step buttons are drawn. */
    public enum SpinButtonPlacement {
        /** A "-" and a "+" button at the end of the field. */
        INLINE,
        /** A narrow stack of up and down arrows. */
        COMPACT,
        /** No buttons (the keyboard and the mouse wheel still step). */
        HIDDEN
    }

    private static final PseudoClass FOCUS_WITHIN = PseudoClass.getPseudoClass("focus-within");
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final DoubleProperty value = new SimpleDoubleProperty(0);
    private final DoubleProperty minimum = new SimpleDoubleProperty(Double.NEGATIVE_INFINITY);
    private final DoubleProperty maximum = new SimpleDoubleProperty(Double.POSITIVE_INFINITY);
    private final DoubleProperty smallChange = new SimpleDoubleProperty(1);
    private final DoubleProperty largeChange = new SimpleDoubleProperty(10);
    private final BooleanProperty wrapEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty nullable = new SimpleBooleanProperty(false);
    private final BooleanProperty acceptsExpression = new SimpleBooleanProperty(true);
    private final IntegerProperty fractionDigits = new SimpleIntegerProperty(-1);
    private final StringProperty prefix = new SimpleStringProperty("");
    private final StringProperty suffix = new SimpleStringProperty("");
    private final StringProperty placeholder = new SimpleStringProperty("");
    private final ObjectProperty<SpinButtonPlacement> spinButtonPlacement =
            new SimpleObjectProperty<>(SpinButtonPlacement.INLINE);

    private final TextField field = new TextField();
    private final HBox inlineButtons = new HBox();
    private final VBox compactButtons = new VBox();
    private final PauseTransition repeatDelay = new PauseTransition(Duration.millis(400));
    private final Timeline repeat = new Timeline();
    private boolean updating;

    public NumberBox() {
        this(0);
    }

    public NumberBox(double initial) {
        getStyleClass().add("tsl-number-box");
        setAlignment(Pos.CENTER_LEFT);
        field.getStyleClass().add("tsl-number-field");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setMinWidth(40);

        inlineButtons.getStyleClass().add("tsl-spin-buttons");
        inlineButtons.getChildren().addAll(spinButton(Symbol.REMOVE, -1, 10), spinButton(Symbol.ADD, 1, 10));
        compactButtons.getStyleClass().addAll("tsl-spin-buttons", "tsl-spin-compact");
        compactButtons.getChildren().addAll(spinButton(Symbol.CHEVRON_UP, 1, 10), spinButton(Symbol.CHEVRON_DOWN, -1, 10));
        getChildren().addAll(field, inlineButtons, compactButtons);

        field.promptTextProperty().bind(placeholder);
        field.focusedProperty().addListener((o, was, is) -> {
            pseudoClassStateChanged(FOCUS_WITHIN, is);
            if (!is) {
                commit();
            }
        });
        field.setOnAction(e -> {
            commit();
            field.selectAll();
        });
        field.setOnKeyPressed(e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.UP) {
                step(smallChange.get());
            } else if (c == KeyCode.DOWN) {
                step(-smallChange.get());
            } else if (c == KeyCode.PAGE_UP) {
                step(largeChange.get());
            } else if (c == KeyCode.PAGE_DOWN) {
                step(-largeChange.get());
            } else if (c == KeyCode.ESCAPE) {
                updateText();
                return;
            } else {
                return;
            }
            e.consume();
        });
        field.setOnScroll(e -> {
            if (field.isFocused() && e.getDeltaY() != 0) {
                step(e.getDeltaY() > 0 ? smallChange.get() : -smallChange.get());
                e.consume();
            }
        });

        value.addListener((o, a, b) -> updateText());
        prefix.addListener((o, a, b) -> updateText());
        suffix.addListener((o, a, b) -> updateText());
        fractionDigits.addListener((o, a, b) -> updateText());
        minimum.addListener((o, a, b) -> setValue(coerce(getValue())));
        maximum.addListener((o, a, b) -> setValue(coerce(getValue())));
        spinButtonPlacement.addListener((o, a, b) -> updatePlacement());
        disabledProperty().addListener((o, a, b) -> stopRepeat());
        updatePlacement();
        setValue(initial);
        updateText();
    }

    public NumberBox(double initial, double min, double max) {
        this(initial);
        setMinimum(min);
        setMaximum(max);
        setValue(initial);
    }

    private Button spinButton(Symbol symbol, int direction, double iconSize) {
        Button b = new Button();
        b.setGraphic(new Icon(symbol, iconSize));
        b.getStyleClass().add("tsl-spin-button");
        b.setFocusTraversable(false);
        b.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            commit();
            step(direction * smallChange.get());
            stopRepeat();
            // hold to repeat, after a short delay
            repeatDelay.setOnFinished(ev -> {
                repeat.getKeyFrames().setAll(
                        new KeyFrame(Duration.millis(60), x -> step(direction * smallChange.get())));
                repeat.setCycleCount(Timeline.INDEFINITE);
                repeat.play();
            });
            repeatDelay.playFromStart();
        });
        b.setOnMouseReleased(e -> stopRepeat());
        b.setOnMouseExited(e -> stopRepeat());
        return b;
    }

    private void stopRepeat() {
        repeatDelay.stop();
        repeat.stop();
    }

    private void updatePlacement() {
        SpinButtonPlacement p = spinButtonPlacement.get();
        inlineButtons.setVisible(p == SpinButtonPlacement.INLINE);
        inlineButtons.setManaged(p == SpinButtonPlacement.INLINE);
        compactButtons.setVisible(p == SpinButtonPlacement.COMPACT);
        compactButtons.setManaged(p == SpinButtonPlacement.COMPACT);
    }

    // ------------------------------------------------------------------ value handling

    /** Steps the value by {@code delta}, wrapping or clamping at the range. */
    public void step(double delta) {
        double v = getValue();
        if (Double.isNaN(v)) {
            v = Double.isInfinite(getMinimum()) ? 0 : getMinimum();
            delta = 0;
        }
        double next = v + delta;
        if (isWrapEnabled() && !Double.isInfinite(getMinimum()) && !Double.isInfinite(getMaximum())) {
            if (next > getMaximum()) {
                next = getMinimum();
            } else if (next < getMinimum()) {
                next = getMaximum();
            }
        }
        setValue(coerce(round(next)));
        updateText();
    }

    private double coerce(double v) {
        if (Double.isNaN(v)) {
            return isNullable() ? Double.NaN : clampDefault();
        }
        return Math.max(getMinimum(), Math.min(getMaximum(), v));
    }

    private double clampDefault() {
        return Math.max(getMinimum(), Math.min(getMaximum(), 0));
    }

    private double round(double v) {
        // avoid 0.1 + 0.2 style noise when stepping by fractions
        return BigDecimal.valueOf(v).setScale(10, RoundingMode.HALF_UP).doubleValue();
    }

    /** Parses and applies the text of the field. */
    private void commit() {
        if (updating) {
            return;
        }
        String text = field.getText() == null ? "" : field.getText().trim();
        text = strip(text);
        if (text.isEmpty()) {
            if (isNullable()) {
                setValue(Double.NaN);
            }
            updateText();
            return;
        }
        try {
            double v = isAcceptsExpression() ? Expression.evaluate(text) : parseNumber(text);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new NumberFormatException(text);
            }
            setValue(coerce(round(v)));
        } catch (RuntimeException ex) {
            flashInvalid();
        }
        updateText();
    }

    private String strip(String text) {
        String p = getPrefix();
        String s = getSuffix();
        if (!p.isEmpty() && text.startsWith(p.trim())) {
            text = text.substring(p.trim().length()).trim();
        }
        if (!s.isEmpty() && text.endsWith(s.trim())) {
            text = text.substring(0, text.length() - s.trim().length()).trim();
        }
        return text;
    }

    private static double parseNumber(String text) {
        return Double.parseDouble(Expression.normalize(text));
    }

    private void flashInvalid() {
        pseudoClassStateChanged(INVALID, true);
        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> pseudoClassStateChanged(INVALID, false));
        pause.play();
    }

    private void updateText() {
        updating = true;
        try {
            double v = getValue();
            field.setText(Double.isNaN(v) ? "" : getPrefix() + format(v) + getSuffix());
        } finally {
            updating = false;
        }
    }

    private String format(double v) {
        int digits = getFractionDigits();
        BigDecimal d = BigDecimal.valueOf(v);
        if (digits >= 0) {
            return d.setScale(digits, RoundingMode.HALF_UP).toPlainString();
        }
        d = d.setScale(Math.min(Math.max(d.scale(), 0), 10), RoundingMode.HALF_UP).stripTrailingZeros();
        return d.toPlainString();
    }

    // ------------------------------------------------------------------ properties

    public final double getValue() {
        return value.get();
    }

    /** Sets the value; it is clamped to the range. {@code NaN} clears the box when it is nullable. */
    public final void setValue(double value) {
        this.value.set(coerce(value));
    }

    public final DoubleProperty valueProperty() {
        return value;
    }

    public final double getMinimum() {
        return minimum.get();
    }

    public final void setMinimum(double minimum) {
        this.minimum.set(minimum);
    }

    public final DoubleProperty minimumProperty() {
        return minimum;
    }

    public final double getMaximum() {
        return maximum.get();
    }

    public final void setMaximum(double maximum) {
        this.maximum.set(maximum);
    }

    public final DoubleProperty maximumProperty() {
        return maximum;
    }

    /** The change of the arrow keys, the mouse wheel and the step buttons. */
    public final double getSmallChange() {
        return smallChange.get();
    }

    public final void setSmallChange(double smallChange) {
        this.smallChange.set(smallChange);
    }

    public final DoubleProperty smallChangeProperty() {
        return smallChange;
    }

    /** The change of Page Up / Page Down. */
    public final double getLargeChange() {
        return largeChange.get();
    }

    public final void setLargeChange(double largeChange) {
        this.largeChange.set(largeChange);
    }

    public final DoubleProperty largeChangeProperty() {
        return largeChange;
    }

    public final boolean isWrapEnabled() {
        return wrapEnabled.get();
    }

    public final void setWrapEnabled(boolean wrapEnabled) {
        this.wrapEnabled.set(wrapEnabled);
    }

    public final BooleanProperty wrapEnabledProperty() {
        return wrapEnabled;
    }

    /** True when the box may be empty (the value is then NaN). */
    public final boolean isNullable() {
        return nullable.get();
    }

    public final void setNullable(boolean nullable) {
        this.nullable.set(nullable);
    }

    public final BooleanProperty nullableProperty() {
        return nullable;
    }

    public final boolean isAcceptsExpression() {
        return acceptsExpression.get();
    }

    public final void setAcceptsExpression(boolean acceptsExpression) {
        this.acceptsExpression.set(acceptsExpression);
    }

    public final BooleanProperty acceptsExpressionProperty() {
        return acceptsExpression;
    }

    /** Digits after the decimal point, or -1 to show as many as needed (up to 10). */
    public final int getFractionDigits() {
        return fractionDigits.get();
    }

    public final void setFractionDigits(int fractionDigits) {
        this.fractionDigits.set(fractionDigits);
    }

    public final IntegerProperty fractionDigitsProperty() {
        return fractionDigits;
    }

    public final String getPrefix() {
        return prefix.get();
    }

    public final void setPrefix(String prefix) {
        this.prefix.set(prefix == null ? "" : prefix);
    }

    public final StringProperty prefixProperty() {
        return prefix;
    }

    public final String getSuffix() {
        return suffix.get();
    }

    public final void setSuffix(String suffix) {
        this.suffix.set(suffix == null ? "" : suffix);
    }

    public final StringProperty suffixProperty() {
        return suffix;
    }

    /** Text shown while the box is empty. */
    public final String getPlaceholder() {
        return placeholder.get();
    }

    public final void setPlaceholder(String placeholder) {
        this.placeholder.set(placeholder == null ? "" : placeholder);
    }

    public final StringProperty placeholderProperty() {
        return placeholder;
    }

    public final SpinButtonPlacement getSpinButtonPlacement() {
        return spinButtonPlacement.get();
    }

    public final void setSpinButtonPlacement(SpinButtonPlacement placement) {
        spinButtonPlacement.set(placement);
    }

    public final ObjectProperty<SpinButtonPlacement> spinButtonPlacementProperty() {
        return spinButtonPlacement;
    }

    /** The text field inside the box. */
    public final TextField getEditor() {
        return field;
    }

    @Override
    public void requestFocus() {
        field.requestFocus();
    }
}
