package net.tessel.fx.icons;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Turns the SVG subset of {@link IconData} into JavaFX shapes. {@code currentColor} becomes the CSS looked-up color
 * {@code -tsl-icon-color}, so icons follow the text color of the control they sit in.
 */
final class IconSvg {

    private static final Pattern TAG = Pattern.compile("<(\\w+)\\b([^>]*?)/?>(?:([^<]*)</text>)?");
    private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");

    private IconSvg() {
    }

    static boolean exists(String key) {
        return IconData.PATHS.containsKey(key);
    }

    /** The shapes of the glyph, in a 20x20 coordinate space. Empty when the key is unknown. */
    static List<Node> build(String key) {
        List<Node> nodes = new ArrayList<>();
        String markup = IconData.PATHS.get(key);
        if (markup == null) {
            return nodes;
        }
        boolean stroked = !key.startsWith("sev-");
        Matcher m = TAG.matcher(markup);
        while (m.find()) {
            String tag = m.group(1);
            Attrs a = new Attrs(m.group(2));
            Shape s;
            switch (tag) {
                case "path":
                    SVGPath p = new SVGPath();
                    p.setContent(a.str("d", ""));
                    s = p;
                    break;
                case "line":
                    s = new Line(a.num("x1"), a.num("y1"), a.num("x2"), a.num("y2"));
                    break;
                case "circle":
                    s = new Circle(a.num("cx"), a.num("cy"), a.num("r"));
                    break;
                case "ellipse":
                    s = new Ellipse(a.num("cx"), a.num("cy"), a.num("rx"), a.num("ry"));
                    break;
                case "rect":
                    Rectangle r = new Rectangle(a.num("x"), a.num("y"), a.num("width"), a.num("height"));
                    double rx = a.num("rx");
                    r.setArcWidth(rx * 2);
                    r.setArcHeight((a.has("ry") ? a.num("ry") : rx) * 2);
                    s = r;
                    break;
                case "text":
                    Text t = new Text(m.group(3) == null ? "" : m.group(3));
                    t.setFont(Font.font(null, FontWeight.SEMI_BOLD, a.num("font-size")));
                    t.setX(a.num("x") - t.getLayoutBounds().getWidth() / 2);
                    t.setY(a.num("y"));
                    t.getStyleClass().add("tsl-ic-fill");
                    nodes.add(t);
                    continue;
                default:
                    continue;
            }
            paint(s, a, stroked);
            nodes.add(s);
        }
        return nodes;
    }

    private static void paint(Shape s, Attrs a, boolean stroked) {
        String fill = a.str("fill", "none");
        String stroke = a.str("stroke", stroked ? "currentColor" : "none");
        applyPaint(s, fill, true);
        applyPaint(s, stroke, false);
        if (!"none".equals(stroke)) {
            s.setStrokeWidth(a.has("stroke-width") ? a.num("stroke-width") : 1.6);
            s.setStrokeLineCap("butt".equals(a.str("stroke-linecap", stroked ? "round" : "butt"))
                    ? StrokeLineCap.BUTT : StrokeLineCap.ROUND);
            s.setStrokeLineJoin("miter".equals(a.str("stroke-linejoin", stroked ? "round" : "miter"))
                    ? StrokeLineJoin.MITER : StrokeLineJoin.ROUND);
        }
    }

    private static void applyPaint(Shape s, String value, boolean fill) {
        if ("none".equals(value)) {
            if (fill) {
                s.setFill(null);
            } else {
                s.setStroke(null);
            }
        } else if ("currentColor".equals(value)) {
            s.getStyleClass().add(fill ? "tsl-ic-fill" : "tsl-ic-stroke");
            if (fill) {
                s.setFill(Color.BLACK); // replaced by the style sheet
            } else {
                s.setStroke(Color.BLACK);
            }
        } else {
            Color c = Color.web(value);
            if (fill) {
                s.setFill(c);
            } else {
                s.setStroke(c);
            }
        }
    }

    private static final class Attrs {
        private final java.util.Map<String, String> map = new java.util.HashMap<>();

        Attrs(String text) {
            Matcher m = ATTR.matcher(text);
            while (m.find()) {
                map.put(m.group(1), m.group(2));
            }
        }

        boolean has(String name) {
            return map.containsKey(name);
        }

        String str(String name, String def) {
            return map.getOrDefault(name, def);
        }

        double num(String name) {
            String v = map.get(name);
            return v == null ? 0 : Double.parseDouble(v);
        }
    }
}
