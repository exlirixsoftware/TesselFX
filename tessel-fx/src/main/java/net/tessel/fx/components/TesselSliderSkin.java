package net.tessel.fx.components;

import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.Region;

/**
 * The skin Tessel gives every {@link Slider} (through the style sheet): the standard slider plus an accent-colored fill
 * from the start of the track to the thumb. It is public only because the style sheet instantiates it by name.
 */
public class TesselSliderSkin extends SliderSkin {

    private final Region fill = new Region();

    public TesselSliderSkin(Slider slider) {
        super(slider);
        fill.getStyleClass().add("tsl-slider-fill");
        fill.setMouseTransparent(true);
        int at = 0;
        for (int i = 0; i < getChildren().size(); i++) {
            if (getChildren().get(i).getStyleClass().contains("track")) {
                at = i + 1;
            }
        }
        getChildren().add(at, fill);
        registerChangeListener(slider.valueProperty(), v -> slider.requestLayout());
        registerChangeListener(slider.orientationProperty(), v -> slider.requestLayout());
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        Node track = null;
        Node thumb = null;
        for (Node n : getChildren()) {
            if (n.getStyleClass().contains("track")) {
                track = n;
            } else if (n.getStyleClass().contains("thumb")) {
                thumb = n;
            }
        }
        if (track == null || thumb == null) {
            return;
        }
        double tx = track.getLayoutX();
        double ty = track.getLayoutY();
        double tw = track.getLayoutBounds().getWidth();
        double th = track.getLayoutBounds().getHeight();
        if (getSkinnable().getOrientation() == javafx.geometry.Orientation.HORIZONTAL) {
            double end = thumb.getLayoutX() + thumb.getLayoutBounds().getWidth() / 2;
            fill.resizeRelocate(tx, ty, Math.max(0, Math.min(tw, end - tx)), th);
        } else {
            double start = thumb.getLayoutY() + thumb.getLayoutBounds().getHeight() / 2;
            fill.resizeRelocate(tx, start, tw, Math.max(0, Math.min(th, ty + th - start)));
        }
    }
}
