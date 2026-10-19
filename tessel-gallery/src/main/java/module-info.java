/** The Tessel gallery: every control and component on one page each. */
module net.tessel.fx.gallery {
    requires net.tessel.fx;
    requires javafx.swing;   // the --screenshots option

    // JavaFX creates the Application subclass by reflection
    exports net.tessel.fx.gallery to javafx.graphics;
}
