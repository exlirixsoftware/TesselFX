package net.tessel.fx.components;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

/**
 * The overlay layer of a scene: a transparent stack pane above the window's content where in-window dialogs,
 * snackbars and teaching tips are drawn. A {@link TesselWindow} has one built in; any other scene gets one the first
 * time it is needed, by wrapping its root.
 */
final class Overlays {

    static final String LAYER_KEY = "net.tessel.fx.overlay";

    private Overlays() {
    }

    /** The scene of an owner given as a node, window or scene. */
    static Scene sceneOf(Object owner) {
        Scene scene = null;
        if (owner instanceof Node) {
            scene = ((Node) owner).getScene();
        } else if (owner instanceof Window) {
            scene = ((Window) owner).getScene();
        } else if (owner instanceof Scene) {
            scene = (Scene) owner;
        }
        if (scene == null) {
            throw new IllegalArgumentException("The owner must be a node, window or scene that is part of a scene");
        }
        return scene;
    }

    static StackPane layer(Scene scene) {
        Object existing = scene.getProperties().get(LAYER_KEY);
        if (existing instanceof StackPane && ((StackPane) existing).getScene() == scene) {
            return (StackPane) existing;
        }
        Parent old = scene.getRoot();
        StackPane host = new StackPane();
        host.getStyleClass().add("tsl-overlay-host");
        StackPane layer = new StackPane();
        layer.getStyleClass().add("tsl-overlay-layer");
        layer.setPickOnBounds(false);
        scene.setRoot(new Pane()); // detach the old root so it can become a child
        host.getChildren().addAll(old, layer);
        scene.setRoot(host);
        scene.getProperties().put(LAYER_KEY, layer);
        return layer;
    }

    static StackPane layer(Object owner) {
        return layer(sceneOf(owner));
    }
}
