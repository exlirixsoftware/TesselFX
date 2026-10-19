package net.tessel.fx;

/**
 * Typography ramp. Apply with {@link Tessel#textStyle(javafx.scene.Node, TextStyle)} or create a label with
 * {@link Tessel#label(String, TextStyle)}.
 */
public enum TextStyle {
    DISPLAY(40, true, false, "tsl-text-display"),
    TITLE(28, true, false, "tsl-text-title"),
    SUBTITLE(20, true, false, "tsl-text-subtitle"),
    HEADER(16, true, false, "tsl-text-header"),
    BODY_STRONG(14, true, false, "tsl-text-body-strong"),
    BODY(14, false, false, "tsl-text-body"),
    SECONDARY(14, false, true, "tsl-text-secondary"),
    CAPTION(12, false, true, "tsl-text-caption"),
    CODE(13, false, false, "tsl-text-code");

    private final double size;
    private final boolean semibold;
    private final boolean secondary;
    private final String styleClass;

    TextStyle(double size, boolean semibold, boolean secondary, String styleClass) {
        this.size = size;
        this.semibold = semibold;
        this.secondary = secondary;
        this.styleClass = styleClass;
    }

    /** Font size in pixels. */
    public double size() {
        return size;
    }

    public boolean isSemibold() {
        return semibold;
    }

    /** True when the style uses the secondary text color. */
    public boolean isSecondary() {
        return secondary;
    }

    /** The CSS style class that carries this style's font and color. */
    public String styleClass() {
        return styleClass;
    }
}
