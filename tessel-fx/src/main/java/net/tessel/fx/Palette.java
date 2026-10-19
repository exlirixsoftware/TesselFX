package net.tessel.fx;

import javafx.scene.paint.Color;

/**
 * The colors of a Tessel theme. The values match the Tessel.NET / Tessel4J design tokens.
 * Obtain the active palette with {@link Tessel#palette()}; the same colors are available to style sheets as the
 * looked-up colors {@code -tsl-background}, {@code -tsl-surface}, {@code -tsl-accent}, ...
 */
public final class Palette {

    public final boolean dark;

    public final Color background;
    public final Color surface;
    public final Color surfaceAlt;
    public final Color subtle;
    public final Color border;
    public final Color borderStrong;

    public final Color text;
    public final Color textSecondary;
    public final Color textTertiary;
    public final Color textDisabled;

    public final Color controlBackground;
    public final Color controlHover;
    public final Color controlPressed;
    public final Color inputBackground;
    public final Color overlay;

    public final Color accent;
    public final Color accentHover;
    public final Color accentPressed;
    public final Color accentSubtle;
    public final Color onAccent;

    public final Color success;
    public final Color successSubtle;
    public final Color warning;
    public final Color warningSubtle;
    public final Color danger;
    public final Color dangerHover;
    public final Color dangerSubtle;
    public final Color info;
    public final Color infoSubtle;

    /** Base color of drop shadows (includes alpha). */
    public final Color shadow;

    /**
     * @param dark         true for the dark palette
     * @param customAccent a custom accent color, or null for the built-in indigo accent
     */
    public Palette(boolean dark, Color customAccent) {
        this.dark = dark;

        background = rgb(dark ? 0x0F1115 : 0xF4F5F7);
        surface = rgb(dark ? 0x181B21 : 0xFFFFFF);
        surfaceAlt = rgb(dark ? 0x1D2027 : 0xF9FAFB);
        subtle = rgb(dark ? 0x242832 : 0xEEF0F3);
        border = rgb(dark ? 0x2C313B : 0xE1E4E8);
        borderStrong = rgb(dark ? 0x434B58 : 0xC4CAD3);

        text = rgb(dark ? 0xE8EAED : 0x1B1F24);
        textSecondary = rgb(dark ? 0xA3ACB9 : 0x58626F);
        textTertiary = rgb(dark ? 0x7A8494 : 0x8A93A0);
        textDisabled = rgb(dark ? 0x565E6B : 0xA8AFB9);

        controlBackground = rgb(dark ? 0x20242C : 0xFFFFFF);
        controlHover = rgb(dark ? 0x292E38 : 0xF2F3F5);
        controlPressed = rgb(dark ? 0x313743 : 0xE7E9ED);
        inputBackground = rgb(dark ? 0x14171C : 0xFFFFFF);
        overlay = dark ? Color.rgb(0, 0, 0, 0x99 / 255.0) : Color.rgb(0x10, 0x13, 0x18, 0x66 / 255.0);

        success = rgb(dark ? 0x4ADE80 : 0x15803D);
        successSubtle = rgb(dark ? 0x15291E : 0xE3F8EA);
        warning = rgb(dark ? 0xFBBF24 : 0xB45309);
        warningSubtle = rgb(dark ? 0x33280F : 0xFEF4DC);
        danger = rgb(dark ? 0xEF4444 : 0xDC2626);
        dangerHover = rgb(dark ? 0xF26363 : 0xB91C1C);
        dangerSubtle = rgb(dark ? 0x361A1C : 0xFDE8E8);
        info = rgb(dark ? 0x38BDF8 : 0x0369A1);
        infoSubtle = rgb(dark ? 0x122838 : 0xE3F2FC);

        shadow = Color.rgb(0, 0, 0, (dark ? 110 : 30) / 255.0);

        if (customAccent == null) {
            accent = rgb(dark ? 0x6366F1 : 0x4F46E5);
            accentHover = rgb(dark ? 0x7C7FF4 : 0x4338CA);
            accentPressed = rgb(dark ? 0x5457D6 : 0x3730A3);
            accentSubtle = rgb(dark ? 0x272A52 : 0xEEF0FF);
            onAccent = Color.WHITE;
        } else {
            Color c = Color.color(customAccent.getRed(), customAccent.getGreen(), customAccent.getBlue());
            accent = c;
            accentHover = dark ? mix(c, Color.WHITE, 0.14) : mix(c, Color.BLACK, 0.12);
            accentPressed = dark ? mix(c, Color.BLACK, 0.15) : mix(c, Color.BLACK, 0.24);
            accentSubtle = mix(c, surface, dark ? 0.78 : 0.88);
            onAccent = luminance(c) > 0.5 ? rgb(0x111418) : Color.WHITE;
        }
    }

    /** Linear blend from {@code from} to {@code to}; {@code amount} 0 = from, 1 = to. The result is opaque. */
    public static Color mix(Color from, Color to, double amount) {
        return Color.color(
                from.getRed() + (to.getRed() - from.getRed()) * amount,
                from.getGreen() + (to.getGreen() - from.getGreen()) * amount,
                from.getBlue() + (to.getBlue() - from.getBlue()) * amount);
    }

    /** The same color with a different opacity (0-1). */
    public static Color withAlpha(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, alpha)));
    }

    /** WCAG relative luminance (0 = black, 1 = white). */
    public static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    /** {@code #rrggbb}, or {@code rgba(...)} when the color is not opaque. */
    public static String css(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        if (c.getOpacity() >= 0.999) {
            return String.format("#%02x%02x%02x", r, g, b);
        }
        return String.format(java.util.Locale.ROOT, "rgba(%d, %d, %d, %.3f)", r, g, b, c.getOpacity());
    }

    /** The {@code -tsl-*} looked-up color definitions of this palette (the body of a CSS rule). */
    String variables() {
        StringBuilder sb = new StringBuilder(1536);
        var(sb, "background", background);
        var(sb, "surface", surface);
        var(sb, "surface-alt", surfaceAlt);
        var(sb, "subtle", subtle);
        var(sb, "border", border);
        var(sb, "border-strong", borderStrong);
        var(sb, "text", text);
        var(sb, "text-secondary", textSecondary);
        var(sb, "text-tertiary", textTertiary);
        var(sb, "text-disabled", textDisabled);
        var(sb, "control", controlBackground);
        var(sb, "control-hover", controlHover);
        var(sb, "control-pressed", controlPressed);
        var(sb, "input", inputBackground);
        var(sb, "overlay", overlay);
        var(sb, "accent", accent);
        var(sb, "accent-hover", accentHover);
        var(sb, "accent-pressed", accentPressed);
        var(sb, "accent-subtle", accentSubtle);
        var(sb, "on-accent", onAccent);
        var(sb, "success", success);
        var(sb, "success-subtle", successSubtle);
        var(sb, "warning", warning);
        var(sb, "warning-subtle", warningSubtle);
        var(sb, "danger", danger);
        var(sb, "danger-hover", dangerHover);
        var(sb, "danger-subtle", dangerSubtle);
        var(sb, "info", info);
        var(sb, "info-subtle", infoSubtle);
        var(sb, "shadow", shadow);
        // The default icon color: icons follow the text color of the control that contains them.
        sb.append("  -tsl-icon-color: ").append(css(text)).append(";\n");
        // Re-map the Modena (built-in theme) base colors so unstyled controls fit the palette.
        sb.append("  -fx-base: ").append(css(controlBackground)).append(";\n");
        sb.append("  -fx-background: ").append(css(background)).append(";\n");
        sb.append("  -fx-accent: ").append(css(accent)).append(";\n");
        sb.append("  -fx-focus-color: ").append(css(accent)).append(";\n");
        sb.append("  -fx-faint-focus-color: transparent;\n");
        sb.append("  -fx-control-inner-background: ").append(css(inputBackground)).append(";\n");
        sb.append("  -fx-control-inner-background-alt: ").append(css(surfaceAlt)).append(";\n");
        sb.append("  -fx-text-base-color: ").append(css(text)).append(";\n");
        sb.append("  -fx-text-background-color: ").append(css(text)).append(";\n");
        sb.append("  -fx-text-inner-color: ").append(css(text)).append(";\n");
        sb.append("  -fx-selection-bar: ").append(css(accent)).append(";\n");
        sb.append("  -fx-selection-bar-text: ").append(css(onAccent)).append(";\n");
        sb.append("  -fx-selection-bar-non-focused: ").append(css(accent)).append(";\n");
        sb.append("  -fx-prompt-text-fill: ").append(css(textTertiary)).append(";\n");
        return sb.toString();
    }

    private static void var(StringBuilder sb, String name, Color c) {
        sb.append("  -tsl-").append(name).append(": ").append(css(c)).append(";\n");
    }

    private static double channel(double s) {
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static Color rgb(int rgb) {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
}
