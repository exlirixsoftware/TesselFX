package net.tessel.fx.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import net.tessel.fx.Tessel;
import net.tessel.fx.TextStyle;

/** A wrapping label with a {@link TextStyle} from the Tessel type ramp. */
public class TextBlock extends Label {

    private final ObjectProperty<TextStyle> textStyle = new SimpleObjectProperty<>(TextStyle.BODY);

    public TextBlock(String text) {
        this(text, TextStyle.BODY);
    }

    public TextBlock(String text, TextStyle style) {
        super(text);
        getStyleClass().add("tsl-text-block");
        setWrapText(true);
        setMinHeight(Region.USE_PREF_SIZE); // keeps wrapped text from being cut to one line inside boxes
        textStyle.addListener((o, a, b) -> Tessel.textStyle(this, b));
        setTextStyle(style);
    }

    public final TextStyle getTextStyle() {
        return textStyle.get();
    }

    public final void setTextStyle(TextStyle style) {
        textStyle.set(style);
        Tessel.textStyle(this, style);
    }

    public final ObjectProperty<TextStyle> textStyleProperty() {
        return textStyle;
    }
}
