package net.tessel.fx.components;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Severity;
import net.tessel.fx.Tessel;

/**
 * Brief messages that appear at the bottom of a window and go away by themselves.
 *
 * <pre>{@code
 * Snackbar.show(window, "Saved", "Your changes were saved.", Severity.SUCCESS);
 * }</pre>
 *
 * The message stays while the mouse is over it. Several snackbars stack up.
 */
public final class Snackbar {

    private static final String HOST_KEY = "net.tessel.fx.snackbars";
    private static final Duration DEFAULT_DURATION = Duration.seconds(4);

    private Snackbar() {
    }

    /**
     * @param owner a window, scene or node of the window to show the message in
     */
    public static InfoBar show(Object owner, String title, String message, Severity severity) {
        return show(owner, title, message, severity, DEFAULT_DURATION, null, null);
    }

    public static InfoBar show(Object owner, String title, String message, Severity severity, Duration duration) {
        return show(owner, title, message, severity, duration, null, null);
    }

    /**
     * @param duration   how long the message stays; null or {@link Duration#INDEFINITE} keeps it until it is closed
     * @param actionText the text of an action button, or null for none
     * @param action     what the action button does; the snackbar closes afterwards
     */
    public static InfoBar show(Object owner, String title, String message, Severity severity, Duration duration,
            String actionText, Runnable action) {
        Scene scene = Overlays.sceneOf(owner);
        VBox host = host(scene);

        InfoBar bar = new InfoBar(severity, title, message);
        bar.setClosable(true);
        bar.setElevated(true);
        bar.setMaxWidth(480);
        bar.setPrefWidth(420);
        if (actionText != null && action != null) {
            Button button = Tessel.style(new Button(actionText), ButtonStyle.SUBTLE);
            button.setOnAction(e -> {
                action.run();
                bar.dismiss();
            });
            bar.setAction(button);
        }
        host.getChildren().add(bar);

        bar.setOpacity(0);
        bar.setTranslateY(24);
        FadeTransition fade = new FadeTransition(Duration.millis(220), bar);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), bar);
        slide.setToY(0);
        slide.setInterpolator(Collapsible.EASE);
        new ParallelTransition(fade, slide).play();

        if (duration != null && !duration.isIndefinite()) {
            PauseTransition timer = new PauseTransition(duration);
            timer.setOnFinished(e -> {
                if (bar.getParent() != null) {
                    bar.dismiss();
                }
            });
            bar.setOnMouseEntered(e -> timer.pause());
            bar.setOnMouseExited(e -> timer.play());
            timer.play();
        }
        return bar;
    }

    private static VBox host(Scene scene) {
        StackPane layer = Overlays.layer(scene);
        Object existing = layer.getProperties().get(HOST_KEY);
        if (existing instanceof VBox) {
            return (VBox) existing;
        }
        VBox host = new VBox(8);
        host.getStyleClass().add("tsl-snackbar-host");
        host.setAlignment(Pos.BOTTOM_CENTER);
        host.setPickOnBounds(false);
        host.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(host, Pos.BOTTOM_CENTER);
        StackPane.setMargin(host, new Insets(24));
        layer.getChildren().add(host);
        layer.getProperties().put(HOST_KEY, host);
        return host;
    }
}
