package net.tessel.fx.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;

/**
 * A small pill-shaped label for a status or a count.
 *
 * <pre>{@code
 * new Badge("New", Badge.Kind.SUCCESS);
 * }</pre>
 */
public class Badge extends Label {

    /** The color of a badge. */
    public enum Kind {
        NEUTRAL("tsl-badge-neutral"),
        ACCENT("tsl-badge-accent"),
        SUCCESS("tsl-badge-success"),
        WARNING("tsl-badge-warning"),
        DANGER("tsl-badge-danger"),
        INFO("tsl-badge-info");

        private final String styleClass;

        Kind(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    private final ObjectProperty<Kind> kind = new SimpleObjectProperty<>(Kind.NEUTRAL);
    private final BooleanProperty solid = new SimpleBooleanProperty(false);

    public Badge(String text) {
        this(text, Kind.NEUTRAL);
    }

    public Badge(String text, Kind kind) {
        super(text);
        getStyleClass().addAll("tsl-badge", "tsl-semibold");
        this.kind.addListener((o, a, b) -> {
            getStyleClass().remove(a.styleClass);
            getStyleClass().add(b.styleClass);
        });
        this.solid.addListener((o, a, b) -> {
            if (b) {
                getStyleClass().add("tsl-solid");
            } else {
                getStyleClass().remove("tsl-solid");
            }
        });
        getStyleClass().add(Kind.NEUTRAL.styleClass);
        setKind(kind == null ? Kind.NEUTRAL : kind);
    }

    public final Kind getKind() {
        return kind.get();
    }

    public final void setKind(Kind kind) {
        this.kind.set(kind);
    }

    public final ObjectProperty<Kind> kindProperty() {
        return kind;
    }

    /** True for a filled badge (white text on the strong color) instead of the subtle tint. */
    public final boolean isSolid() {
        return solid.get();
    }

    public final void setSolid(boolean solid) {
        this.solid.set(solid);
    }

    public final BooleanProperty solidProperty() {
        return solid;
    }
}
