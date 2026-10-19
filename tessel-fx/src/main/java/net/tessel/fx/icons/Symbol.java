package net.tessel.fx.icons;

/**
 * The glyph catalog of Tessel (the same names as Tessel4J's {@code Symbol} and Tessel.js). Use with {@link Icon}.
 * Every glyph is vector-drawn, so they look the same on Windows, macOS and Linux and need no icon font.
 */
public enum Symbol {
    GLOBAL_NAV, WIFI, BLUETOOTH, BRIGHTNESS, QUIET_HOURS,
    CHEVRON_DOWN, CHEVRON_UP, EDIT, ADD, CANCEL, MORE,
    SETTINGS, VIDEO, MAIL, PEOPLE, PHONE, PIN, SHOP,
    LINK, FILTER, SEARCH, CAMERA, ATTACH, SEND,
    FORWARD, BACK, REFRESH, SHARE, LOCK, FAVORITE_STAR,
    FAVORITE_STAR_FILL, REMOVE, CHECKBOX_COMPOSITE, CHECK_MARK, PRINT,
    UP, DOWN, DELETE, SAVE, CLOUD, KEYBOARD, PLAY,
    PAUSE, CHEVRON_LEFT, CHEVRON_RIGHT, EMOJI, GLOBE, CONTACT,
    PASTE, UNLOCK, CALENDAR, COLOR, REDO, UNDO,
    WARNING, FLAG, PAGE, TOUCH_POINTER, HOME, CLOCK,
    VIEW, CLEAR, SYNC, DOWNLOAD, HELP, UPLOAD,
    DOCUMENT, VIEW_ALL, RENAME, FOLDER, CHROME_CLOSE,
    MESSAGE, CUT, COPY, SORT, FONT, TAG, LIBRARY,
    ACCEPT, COMMENT, PICTURES, CHROME_MINIMIZE, CHROME_MAXIMIZE,
    CHROME_RESTORE, COMPLETED, CODE, INFO, SHIELD, LIST,
    ERROR_BADGE, LIGHTBULB, RINGER, HEART, BUG,
    /** A page with a folded corner. */
    FILE,
    /** File chooser glyphs. */
    DRIVE, DESKTOP, NEW_FOLDER, VIEW_DETAILS;

    /** The key of this glyph in the icon table (the lower-case name with dashes). */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
