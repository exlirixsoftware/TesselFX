package net.tessel.fx;

import javafx.scene.paint.Color;

/** Severity of an info bar, snackbar or status icon. */
public enum Severity {
    INFORMATIONAL("tsl-informational", "informational"),
    SUCCESS("tsl-success", "success"),
    WARNING("tsl-warning", "warning"),
    ERROR("tsl-error", "error");

    private final String styleClass;
    private final String iconKey;

    Severity(String styleClass, String iconKey) {
        this.styleClass = styleClass;
        this.iconKey = iconKey;
    }

    /** The CSS style class that selects this severity's colors. */
    public String styleClass() {
        return styleClass;
    }

    /** The key of the filled status glyph of this severity in the icon set. */
    public String iconKey() {
        return "sev-" + iconKey;
    }

    /** The strong color of this severity in the given palette. */
    public Color color(Palette p) {
        switch (this) {
            case SUCCESS: return p.success;
            case WARNING: return p.warning;
            case ERROR: return p.danger;
            default: return p.info;
        }
    }

    /** The subtle (background) color of this severity in the given palette. */
    public Color subtleColor(Palette p) {
        switch (this) {
            case SUCCESS: return p.successSubtle;
            case WARNING: return p.warningSubtle;
            case ERROR: return p.dangerSubtle;
            default: return p.infoSubtle;
        }
    }
}
