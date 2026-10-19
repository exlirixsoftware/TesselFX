package net.tessel.fx.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * An on/off switch with an optional label. It is a {@link ToggleButton}, so {@code selected}, {@code onAction} and
 * toggle groups work as usual.
 *
 * <pre>{@code
 * ToggleSwitch wifi = new ToggleSwitch("Wi-Fi", true);
 * wifi.selectedProperty().addListener((o, was, is) -> ...);
 * }</pre>
 */
public class ToggleSwitch extends ToggleButton {

    private static final double OFF_X = 4;
    private static final double ON_X = 24;

    private final StackPane track = new StackPane();
    private final Circle thumb = new Circle(6);
    private Timeline animation;

    public ToggleSwitch() {
        this(null, false);
    }

    public ToggleSwitch(String text) {
        this(text, false);
    }

    public ToggleSwitch(String text, boolean selected) {
        super(text);
        getStyleClass().setAll("tsl-switch");
        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);
        setMnemonicParsing(false);

        track.getStyleClass().add("tsl-switch-track");
        thumb.getStyleClass().add("tsl-switch-thumb");
        track.setMinSize(40, 20);
        track.setPrefSize(40, 20);
        track.setMaxSize(40, 20);
        track.setAlignment(Pos.CENTER_LEFT);
        track.getChildren().add(thumb);
        thumb.setMouseTransparent(true);
        setGraphic(track);

        selectedProperty().addListener((o, a, b) -> move(b, true));
        setSelected(selected);
        move(selected, false);
    }

    private void move(boolean on, boolean animate) {
        double target = on ? ON_X : OFF_X;
        if (animation != null) {
            animation.stop();
        }
        if (!animate) {
            thumb.setTranslateX(target);
            return;
        }
        animation = new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(thumb.translateXProperty(), target, Collapsible.EASE)));
        animation.play();
    }
}
