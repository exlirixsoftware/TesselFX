package net.tessel.fx.components;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import net.tessel.fx.ProgressState;
import net.tessel.fx.Tessel;

/**
 * A circular progress indicator. Indeterminate by default; give it a {@link #setProgress(double) progress} between
 * 0 and 1 for a determinate ring. {@link ProgressState} turns it paused (warning), failed (danger) or finished (success).
 *
 * <pre>{@code
 * ProgressRing busy = new ProgressRing(32);
 * ProgressRing half = new ProgressRing(48); half.setProgress(0.5);
 * }</pre>
 */
public class ProgressRing extends Region {

    /** The progress value of an indeterminate ring. */
    public static final double INDETERMINATE = -1;

    private final DoubleProperty progress = new SimpleDoubleProperty(INDETERMINATE);
    private final ObjectProperty<ProgressState> state = new SimpleObjectProperty<>(ProgressState.NORMAL);
    private final DoubleProperty phase = new SimpleDoubleProperty();

    private final Circle track = new Circle();
    private final Arc arc = new Arc();
    private final Timeline spin;

    public ProgressRing() {
        this(32);
    }

    public ProgressRing(double size) {
        getStyleClass().add("tsl-progress-ring");
        track.getStyleClass().add("tsl-ring-track");
        track.setFill(null);
        arc.getStyleClass().add("tsl-ring-arc");
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        track.setMouseTransparent(true);
        arc.setMouseTransparent(true);
        getChildren().addAll(track, arc);
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        spin = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(phase, 0, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(1800), new KeyValue(phase, 1, Interpolator.LINEAR)));
        spin.setCycleCount(Animation.INDEFINITE);

        phase.addListener((o, a, b) -> updateArc());
        progress.addListener((o, a, b) -> {
            updateAnimation();
            updateArc();
        });
        state.addListener((o, a, b) -> {
            Tessel.progressState(this, b);
            updateAnimation();
            updateArc();
        });
        sceneProperty().addListener((o, a, b) -> updateAnimation());
        visibleProperty().addListener((o, a, b) -> updateAnimation());
        updateAnimation();
        updateArc();
    }

    private boolean isIndeterminate() {
        return progress.get() < 0;
    }

    private void updateAnimation() {
        boolean run = getScene() != null && isVisible() && isIndeterminate() && !state.get().isFrozen();
        if (run && spin.getStatus() != Animation.Status.RUNNING) {
            spin.play();
        } else if (!run) {
            spin.stop();
        }
    }

    private void updateArc() {
        ProgressState s = state.get();
        double start;
        double sweep;
        if (!isIndeterminate()) {
            start = 90;
            sweep = -360 * Math.min(1, progress.get());
            if (sweep < -359.9) {
                sweep = -359.9;
            }
        } else if (s == ProgressState.PAUSED) {
            start = 90;
            sweep = -200;
        } else if (s.isFrozen()) {
            start = 90;
            sweep = -359.9;
        } else {
            double p = phase.get();
            double rotation = p * 720;
            sweep = 25 + 235 * (0.5 - 0.5 * Math.cos(2 * Math.PI * p));
            start = 90 - rotation;
            sweep = -sweep;
        }
        arc.setStartAngle(start);
        arc.setLength(sweep);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        double stroke = Math.max(2.5, size / 9);
        double r = (size - stroke) / 2;
        double cx = getWidth() / 2;
        double cy = getHeight() / 2;
        track.setCenterX(cx);
        track.setCenterY(cy);
        track.setRadius(r);
        track.setStrokeWidth(stroke);
        arc.setCenterX(cx);
        arc.setCenterY(cy);
        arc.setRadiusX(r);
        arc.setRadiusY(r);
        arc.setStrokeWidth(stroke);
    }

    /** Progress from 0 to 1, or {@link #INDETERMINATE} (any negative value). */
    public final double getProgress() {
        return progress.get();
    }

    public final void setProgress(double progress) {
        this.progress.set(progress);
    }

    public final DoubleProperty progressProperty() {
        return progress;
    }

    public final ProgressState getState() {
        return state.get();
    }

    public final void setState(ProgressState state) {
        this.state.set(state == null ? ProgressState.NORMAL : state);
    }

    public final ObjectProperty<ProgressState> stateProperty() {
        return state;
    }
}
