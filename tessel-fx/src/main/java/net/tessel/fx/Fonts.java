package net.tessel.fx;

import java.util.HashSet;
import java.util.Set;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Font helpers. Uses Segoe UI on Windows and sensible fallbacks elsewhere. */
public final class Fonts {

    private static Set<String> families;
    private static String ui;
    private static String semibold;
    private static String mono;
    private static boolean resolved;

    private Fonts() {
    }

    /** True when a font family with this name is installed. */
    public static synchronized boolean isInstalled(String family) {
        if (families == null) {
            families = new HashSet<>(Font.getFamilies());
        }
        return families.contains(family);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        ui = first("Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Cantarell", "Ubuntu", "Noto Sans", "DejaVu Sans");
        if (ui == null) {
            ui = "System";
        }
        semibold = first("Segoe UI Semibold", "Inter SemiBold");
        mono = first("Cascadia Mono", "Consolas", "JetBrains Mono", "SF Mono", "Menlo", "DejaVu Sans Mono");
        if (mono == null) {
            mono = "Monospaced";
        }
        resolved = true;
    }

    private static String first(String... names) {
        for (String n : names) {
            if (isInstalled(n)) {
                return n;
            }
        }
        return null;
    }

    /** The UI font family. */
    public static String uiFamily() {
        resolve();
        return ui;
    }

    /** A dedicated semibold family (such as "Segoe UI Semibold"), or null when only the bold weight exists. */
    public static String semiboldFamily() {
        resolve();
        return semibold;
    }

    public static String monoFamily() {
        resolve();
        return mono;
    }

    public static Font regular(double size) {
        resolve();
        return Font.font(ui, size);
    }

    public static Font semibold(double size) {
        resolve();
        return semibold != null ? Font.font(semibold, size) : Font.font(ui, FontWeight.BOLD, size);
    }

    public static Font mono(double size) {
        resolve();
        return Font.font(mono, size);
    }

    public static Font forStyle(TextStyle style) {
        if (style == TextStyle.CODE) {
            return mono(style.size());
        }
        return style.isSemibold() ? semibold(style.size()) : regular(style.size());
    }
}
