/**
 * Tessel for JavaFX: a Fluent-inspired, themeable design system - the same look as Tessel.NET, Tessel4J and
 * Tessel.js - as a style sheet for every standard JavaFX control plus a set of modern components.
 */
module net.tessel.fx {
    requires transitive javafx.controls;

    exports net.tessel.fx;
    exports net.tessel.fx.components;
    exports net.tessel.fx.icons;
}
