package net.tessel.fx.gallery;

import java.util.Map;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.Tessel;
import net.tessel.fx.TesselTheme;
import net.tessel.fx.components.NavigationView;
import net.tessel.fx.components.TesselWindow;
import net.tessel.fx.gallery.pages.ButtonsPage;
import net.tessel.fx.gallery.pages.CollectionsPage;
import net.tessel.fx.gallery.pages.DesignPage;
import net.tessel.fx.gallery.pages.DialogsPage;
import net.tessel.fx.gallery.pages.FeedbackPage;
import net.tessel.fx.gallery.pages.FilesPage;
import net.tessel.fx.gallery.pages.HomePage;
import net.tessel.fx.gallery.pages.IconsPage;
import net.tessel.fx.gallery.pages.InputsPage;
import net.tessel.fx.gallery.pages.LayoutPage;
import net.tessel.fx.gallery.pages.MenusPage;
import net.tessel.fx.gallery.pages.PickersPage;
import net.tessel.fx.gallery.pages.SelectionPage;
import net.tessel.fx.gallery.pages.SettingsPage;
import net.tessel.fx.gallery.pages.TabsPage;
import net.tessel.fx.icons.Icon;
import net.tessel.fx.icons.Symbol;

/**
 * The Tessel gallery: every control and component on one page each.
 *
 * <p>Options (JavaFX named parameters): {@code --theme=system|light|dark}, {@code --page=<index>},
 * {@code --screenshots=<directory>} (writes a PNG of every page in both themes and exits; needs javafx.swing).
 */
public class TesselGallery extends Application {
    @Override
    public void start(Stage primary) {
        Map<String, String> options = getParameters().getNamed();
        Tessel.setup(parseTheme(options.get("theme")));

        TesselWindow window = new TesselWindow("TesselFX Gallery");
        for (int size : new int[]{16, 32, 48, 256}) {
            window.getIcons().add(new Image(TesselGallery.class.getResourceAsStream("icon-" + size + ".png")));
        }
        window.setIconSize(18);

        NavigationView nav = new NavigationView("TesselFX Gallery");
        nav.addItem("Home", Symbol.HOME, HomePage::new);
        nav.addHeader("Controls");
        nav.addItem("Buttons", Symbol.TOUCH_POINTER, ButtonsPage::new);
        nav.addItem("Inputs", Symbol.EDIT, InputsPage::new);
        nav.addItem("Selection", Symbol.CHECKBOX_COMPOSITE, SelectionPage::new);
        nav.addItem("Collections", Symbol.LIST, CollectionsPage::new);
        nav.addItem("Menus", Symbol.GLOBAL_NAV, MenusPage::new);
        nav.addHeader("Components");
        nav.addItem("Feedback", Symbol.INFO, FeedbackPage::new);
        nav.addItem("Dialogs", Symbol.MESSAGE, DialogsPage::new);
        nav.addItem("Pickers", Symbol.COLOR, PickersPage::new);
        nav.addItem("Files", Symbol.FOLDER, FilesPage::new);
        nav.addItem("Tabs", Symbol.LIBRARY, TabsPage::new);
        nav.addItem("Layout", Symbol.VIEW_ALL, LayoutPage::new);
        nav.addHeader("Design");
        nav.addItem("Palette", Symbol.TAG, DesignPage::new);
        nav.addItem("Icons", Symbol.EMOJI, IconsPage::new);
        nav.addFooterItem("Settings", Symbol.SETTINGS, SettingsPage::new);

        Button toggle = Tessel.style(new Button(), ButtonStyle.ICON);
        toggle.setGraphic(new Icon(Symbol.BRIGHTNESS, 18));
        toggle.setTooltip(new Tooltip("Switch between light and dark"));
        toggle.setOnAction(e -> Tessel.setTheme(Tessel.isDark() ? TesselTheme.LIGHT : TesselTheme.DARK));
        nav.addTopBarComponent(toggle);

        nav.select(parseInt(options.get("page"), 0));
        window.setContent(nav);
        window.setContentSize(1180, 800);
        window.setContentMinSize(420, 360);
        window.centerOnScreen();
        window.show();

        String shots = options.get("screenshots");
        if (shots != null) {
            Screenshots.run(window, nav, shots);
        }
    }

    private static TesselTheme parseTheme(String text) {
        if (text == null) {
            return TesselTheme.SYSTEM;
        }
        switch (text.toLowerCase()) {
            case "dark":
                return TesselTheme.DARK;
            case "light":
                return TesselTheme.LIGHT;
            default:
                return TesselTheme.SYSTEM;
        }
    }

    private static int parseInt(String text, int def) {
        try {
            return text == null ? def : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
