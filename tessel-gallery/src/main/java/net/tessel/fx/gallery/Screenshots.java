package net.tessel.fx.gallery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import net.tessel.fx.Tessel;
import net.tessel.fx.TesselTheme;
import net.tessel.fx.components.NavigationView;
import net.tessel.fx.components.TesselWindow;

/** Writes a PNG of every gallery page in the light and the dark theme, then exits. */
final class Screenshots {

    private Screenshots() {
    }

    static void run(TesselWindow window, NavigationView nav, String directory) {
        File dir = new File(directory);
        dir.mkdirs();
        List<Runnable> steps = new ArrayList<>();
        for (TesselTheme theme : new TesselTheme[]{TesselTheme.LIGHT, TesselTheme.DARK}) {
            for (NavigationView.Item item : nav.getItems()) {
                steps.add(() -> {
                    Tessel.setTheme(theme);
                    nav.select(item);
                });
                steps.add(() -> save(window, new File(dir, item.getTitle().toLowerCase() + "-" + theme.name().toLowerCase() + ".png")));
            }
        }
        next(steps, 0);
    }

    private static void next(List<Runnable> steps, int index) {
        if (index >= steps.size()) {
            Platform.exit();
            return;
        }
        PauseTransition pause = new PauseTransition(Duration.millis(index == 0 ? 900 : 650));
        pause.setOnFinished(e -> {
            steps.get(index).run();
            next(steps, index + 1);
        });
        pause.play();
    }

    private static void save(TesselWindow window, File file) {
        WritableImage image = window.getScene().snapshot(null);
        try {
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            System.out.println("Wrote " + file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
