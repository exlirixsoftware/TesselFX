package net.tessel.fx;

import javafx.scene.paint.Color;

/**
 * The state of a progress bar or progress ring: the normal accent color, paused (warning), failed (error) or
 * finished (success). Set it with {@link Tessel#progressState(javafx.scene.Node, ProgressState)} on a
 * {@code ProgressBar} / {@code ProgressIndicator}, or with {@code ProgressRing.setState}.
 *
 * <p>A paused or failed indeterminate indicator stops animating.
 */
public enum ProgressState {
    /** Running: the accent color. */
    NORMAL(null),
    /** Paused by the user or the system: the warning color, animation stopped. */
    PAUSED("tsl-paused"),
    /** Failed: the danger color, animation stopped. */
    ERROR("tsl-error"),
    /** Finished: the success color. */
    SUCCESS("tsl-success");

    private final String styleClass;

    ProgressState(String styleClass) {
        this.styleClass = styleClass;
    }

    /** The CSS style class of this state, or null for {@link #NORMAL}. */
    public String styleClass() {
        return styleClass;
    }

    /** The fill color of this state in the given palette. */
    public Color color(Palette p) {
        switch (this) {
            case PAUSED: return p.warning;
            case ERROR: return p.danger;
            case SUCCESS: return p.success;
            default: return p.accent;
        }
    }

    /** True when an indeterminate indicator in this state does not animate. */
    public boolean isFrozen() {
        return this != NORMAL;
    }
}
