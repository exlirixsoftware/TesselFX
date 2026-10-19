package net.tessel.fx.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A container that shows its content at a fraction of its height and animates between collapsed and open.
 * Used by {@link Expander} and the navigation view.
 */
final class Collapsible extends Region {

    /** The ease-out curve used by all Tessel animations: CSS cubic-bezier(0.16, 1, 0.3, 1). */
    static final Interpolator EASE = new CubicBezier(0.16, 1, 0.3, 1);

    /** A CSS-style cubic bezier easing (JavaFX 26 deprecates Interpolator.SPLINE, which did the same). */
    private static final class CubicBezier extends Interpolator {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        CubicBezier(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private static double bezier(double t, double p1, double p2) {
            double u = 1 - t;
            return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t;
        }

        @Override
        protected double curve(double fraction) {
            if (fraction <= 0 || fraction >= 1) {
                return Math.max(0, Math.min(1, fraction));
            }
            // find the curve parameter whose x is the given fraction (x is monotonic), then read y there
            double low = 0;
            double high = 1;
            double t = fraction;
            for (int i = 0; i < 40; i++) {
                double x = bezier(t, x1, x2);
                if (Math.abs(x - fraction) < 1e-6) {
                    break;
                }
                if (x < fraction) {
                    low = t;
                } else {
                    high = t;
                }
                t = (low + high) / 2;
            }
            return bezier(t, y1, y2);
        }
    }

    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final Rectangle clip = new Rectangle();
    private Node content;
    private Timeline timeline;

    Collapsible() {
        setClip(clip);
        progress.addListener((o, a, b) -> {
            setVisible(b.doubleValue() > 0.001);
            requestLayout();
        });
        setVisible(false);
    }

    void setContent(Node node) {
        if (content != null) {
            getChildren().remove(content);
        }
        content = node;
        if (node != null) {
            getChildren().add(node);
        }
        requestLayout();
    }

    boolean isOpen() {
        return progress.get() > 0.5;
    }

    void set(boolean open, boolean animate) {
        if (timeline != null) {
            timeline.stop();
        }
        double target = open ? 1 : 0;
        if (!animate) {
            progress.set(target);
            return;
        }
        timeline = new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(progress, target, EASE)));
        timeline.play();
    }

    @Override
    protected double computePrefWidth(double height) {
        return content == null ? 0 : content.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        if (content == null) {
            return 0;
        }
        return Math.ceil(content.prefHeight(width) * progress.get());
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        clip.setWidth(w);
        clip.setHeight(h);
        if (content != null) {
            content.resizeRelocate(0, 0, w, content.prefHeight(w));
        }
    }
}
