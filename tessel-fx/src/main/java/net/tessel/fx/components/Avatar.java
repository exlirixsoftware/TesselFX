package net.tessel.fx.components;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * A circular avatar: a picture, or the initials of a name on a color derived from the name.
 *
 * <pre>{@code
 * new Avatar("Ada Lovelace", 40);
 * }</pre>
 */
public class Avatar extends StackPane {

    private static final String[] COLORS = {
            "#4f46e5", "#0891b2", "#059669", "#d97706", "#db2777", "#7c3aed", "#dc2626", "#2563eb"
    };

    private final StringProperty name = new SimpleStringProperty("");
    private final DoubleProperty size = new SimpleDoubleProperty(40);
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();

    private final Label initials = new Label();
    private final ImageView picture = new ImageView();
    private final Circle clip = new Circle();

    public Avatar(String name, double size) {
        getStyleClass().add("tsl-avatar");
        setAlignment(Pos.CENTER);
        initials.getStyleClass().addAll("tsl-avatar-initials", "tsl-semibold");
        picture.setPreserveRatio(false);
        getChildren().addAll(initials, picture);
        picture.setVisible(false);
        setClip(clip);

        this.name.addListener((o, a, b) -> update());
        this.size.addListener((o, a, b) -> update());
        this.image.addListener((o, a, b) -> update());
        setName(name);
        setSize(size);
        update();
    }

    public Avatar(String name) {
        this(name, 40);
    }

    private void update() {
        double s = size.get();
        setMinSize(s, s);
        setPrefSize(s, s);
        setMaxSize(s, s);
        clip.setCenterX(s / 2);
        clip.setCenterY(s / 2);
        clip.setRadius(s / 2);
        String n = name.get();
        setStyle("-fx-background-color: " + (n.isEmpty() ? "-tsl-accent" : COLORS[hash(n) % COLORS.length])
                + "; -fx-background-radius: " + s + ";");
        initials.setStyle("-fx-font-size: " + Math.max(8, Math.round(s * 0.38)) + "px;");
        initials.setText(initialsOf(n));
        Image img = image.get();
        picture.setVisible(img != null);
        initials.setVisible(img == null);
        picture.setImage(img);
        picture.setFitWidth(s);
        picture.setFitHeight(s);
    }

    private static int hash(String name) {
        long h = 0;
        for (int i = 0; i < name.length(); i++) {
            h = (h * 31 + name.charAt(i)) & 0xFFFFFFFFL;
        }
        return (int) (h % Integer.MAX_VALUE);
    }

    private static String initialsOf(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "";
        }
        String first = words[0].substring(0, 1);
        if (words.length == 1) {
            return first.toUpperCase();
        }
        return (first + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    public final String getName() {
        return name.get();
    }

    public final void setName(String name) {
        this.name.set(name == null ? "" : name);
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public final double getSize() {
        return size.get();
    }

    public final void setSize(double size) {
        this.size.set(size);
    }

    public final DoubleProperty sizeProperty() {
        return size;
    }

    /** A picture to show instead of the initials. */
    public final Image getImage() {
        return image.get();
    }

    public final void setImage(Image image) {
        this.image.set(image);
    }

    public final ObjectProperty<Image> imageProperty() {
        return image;
    }
}
