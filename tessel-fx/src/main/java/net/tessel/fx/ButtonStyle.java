package net.tessel.fx;

/**
 * Visual variants of buttons. Apply with {@link Tessel#style(javafx.scene.control.ButtonBase, ButtonStyle)}.
 */
public enum ButtonStyle {
    /** Neutral surface with a border. */
    STANDARD(null),
    /** Filled with the accent color, for the primary action. */
    ACCENT("tsl-accent"),
    /** Accent-colored border and text. */
    OUTLINE("tsl-outline"),
    /** No background until hovered (ghost button). */
    SUBTLE("tsl-subtle"),
    /** Red, for destructive actions. */
    DANGER("tsl-danger"),
    /** Square 32x32 icon-only button. */
    ICON("tsl-icon-button"),
    /** Looks like a hyperlink. */
    LINK("tsl-link");

    private final String styleClass;

    ButtonStyle(String styleClass) {
        this.styleClass = styleClass;
    }

    /** The CSS style class this variant adds to a button, or null for {@link #STANDARD}. */
    public String styleClass() {
        return styleClass;
    }
}
