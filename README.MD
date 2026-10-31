![logo](res/javafx.png)
# TesselFX

A modern, themeable design system for **JavaFX**, matching the design of [Tessel.NET](../Tessel.NET), [Tessel4J](../Tessel4J) and [Tessel.js](../Tessel.js).

- Light, dark and system themes, switchable at runtime - the system theme is followed live on JavaFX 22+
- Custom accent colors (or the Windows accent)
- **One style sheet restyles every standard JavaFX control** - rounded, flat, Fluent-inspired: buttons, text inputs, combo boxes, spinners, sliders, progress bars, check boxes, radio buttons, lists, tables, trees, tab panes, menus, tool tips, tool bars, split panes, titled panes, date and color pickers, `Alert` and `Dialog`
- Modern components: `TesselWindow` (custom title bar), `NavigationView`, `TabView`, `ContentDialog`, `TeachingTip`, `Snackbar`, `InfoBar`, `Card`, `SettingsCard`, `Expander`, `BreadcrumbBar`, `SplitButton`, `DropDownButton`, `NumberBox`, `ColorPickerPane`, `TesselDialog`, `TesselFileChooser` / `FileBrowser`, `ToggleSwitch`, `ProgressRing`, `Badge`, `Avatar`, `TextBlock`, `Icon`
- 104 vector icons that follow the text color and look the same on Windows, macOS and Linux - no icon font needed
- Java 17+, JavaFX 21+, **zero dependencies** besides JavaFX itself

```
TesselFX
├── pom.xml           parent POM (JavaFX version, plugin versions)
├── tessel-fx/        the library: style sheet, theme engine, components (Java module net.tessel.fx)
├── tessel-gallery/   demo app showing every control
├── lib/              JavaFX jars from before the Maven build - not used by it any more
└── res/
```

## Build and run

A [Maven](https://maven.apache.org) project; it needs a **JDK 17 or newer** (it is set up for JDK 26) and, to run the gallery,
a desktop. JavaFX comes from Maven Central (`org.openjfx`), **including its native libraries** for your operating system -
there is nothing to download or configure by hand.

```bash
mvn install                              # builds tessel-fx and the gallery, runs nothing else
mvn -pl tessel-gallery javafx:run        # starts the gallery
```

| Artifact | Use |
| --- | --- |
| `tessel-fx/target/tessel-fx-1.0.0.jar` | the library - a Java module named `net.tessel.fx` |
| `tessel-fx/target/tessel-fx-1.0.0-sources.jar` | sources, for IDE navigation / Javadoc |
| `tessel-gallery/target/tessel-fx-gallery-1.0.0.jar` | the demo app |

Options of the gallery go after `-Djavafx.args=`, for example
`mvn -pl tessel-gallery javafx:run -Djavafx.args="--theme=dark --page=3"`, or
`--screenshots=<dir>` to write a PNG of every page in both themes.

### JavaFX version

`pom.xml` sets `javafx.version` to **26.0.2**, the JavaFX release that goes with JDK 26. The library only uses API that
exists since JavaFX 21 and its class files target Java 17 (`java.release`), so you can build against another JavaFX
version for an older JDK:

```bash
mvn install -Djavafx.version=21.0.11 -Djava.release=17     # JavaFX 21 needs JDK 17+; JavaFX 26 needs JDK 24+
```

The gallery is built for Java 26 (`java.release` in `tessel-gallery/pom.xml`) so that editors pick a JDK that can load
JavaFX 26; the library stays on Java 17. If you run the gallery from an IDE and see `Unsupported major.minor version 68.0`
the IDE is using a JDK older than 24: select JDK 24 or newer for the project, or lower `javafx.version` and `java.release`
as above.

### Using TesselFX in your own project

```xml
<dependency>
    <groupId>io.github.exlirixsoftware</groupId>
    <artifactId>tessel-fx</artifactId>
    <version>1.0.0</version>
</dependency>
```

```kotlin
// Gradle
dependencies { implementation("io.github.exlirixsoftware:tessel-fx:1.0.0") }
```

The library is published on Maven Central (see [RELEASING.md](RELEASING.md) for how a release is made); until the first release
is out, run `mvn install` here and add `mavenLocal()` to the repositories of a Gradle project. It brings `javafx-controls` (and
with it `javafx-graphics` and `javafx-base`) along; declare your own `javafx-controls` version to use another JavaFX release. If
your project is a Java module, add `requires net.tessel.fx;` to its `module-info.java`.

## Getting started

Add `tessel-fx-1.0.0.jar` next to the JavaFX jars (module path, or class path) and set Tessel up when the application starts:

```java
public class MyApp extends Application {
    @Override
    public void start(Stage stage) {
        Tessel.setup(TesselTheme.SYSTEM);                 // or LIGHT / DARK

        stage.setScene(new Scene(new MyContent(), 900, 600));
        stage.show();
    }
}
```

Once Tessel is set up, **every scene of every window** - including context menus, combo box lists, tool tips and dialogs - is themed automatically and follows later theme and accent changes. Call `Tessel.setup` once the JavaFX toolkit is running - from `start` (or `init`), not from `main`.

Running (the library is a module, JavaFX stays on the module path):

```bash
java --module-path <javafx>/lib:tessel-fx-1.0.0.jar --add-modules net.tessel.fx -cp myapp.jar MyApp   # use ; instead of : on Windows
```

For the title bar and navigation, use `TesselWindow` instead of the primary stage:

```java
TesselWindow window = new TesselWindow("My App");
window.getIcons().add(logo);
window.setIconSize(20);                                  // title bar icon size (default 16)
window.addTitleBarComponent(new Button("..."));          // components left of the window buttons

NavigationView nav = new NavigationView("My App");
nav.addItem("Home", Symbol.HOME, HomePage::new);         // pages are created lazily and cached
window.setContent(nav);
window.setContentSize(1100, 720);
window.show();
```

## Theming

```java
Tessel.setTheme(TesselTheme.DARK);        // updates every open window
Tessel.setAccent(Color.web("#0078D4"));   // null = built-in indigo
Tessel.useSystemAccent();                 // Windows accent color
Tessel.addThemeListener(() -> { ... });

Palette p = Tessel.palette();             // the active colors: p.accent, p.surface, p.textSecondary, p.danger, ...
Tessel.paletteProperty();                 // observable, for custom drawing code
```

Palette colors are also available to style sheets as looked-up colors, so your own CSS follows the theme:

```css
.my-panel {
    -fx-background-color: -tsl-surface;
    -fx-border-color: -tsl-border;
    -fx-text-fill: -tsl-text-secondary;
}
```

`-tsl-background`, `-tsl-surface`, `-tsl-surface-alt`, `-tsl-subtle`, `-tsl-border`, `-tsl-border-strong`, `-tsl-text`, `-tsl-text-secondary`, `-tsl-text-tertiary`, `-tsl-text-disabled`, `-tsl-control`, `-tsl-control-hover`, `-tsl-control-pressed`, `-tsl-input`, `-tsl-overlay`, `-tsl-accent`, `-tsl-accent-hover`, `-tsl-accent-pressed`, `-tsl-accent-subtle`, `-tsl-on-accent`, `-tsl-success`, `-tsl-warning`, `-tsl-danger`, `-tsl-info` (and `-subtle` variants), `-tsl-shadow`.

Stylesheets you add to a scene take precedence over Tessel's, because Tessel inserts its own first. Put `Tessel.IGNORE` into `scene.getProperties()` to keep a scene out of automatic theming, or use `Tessel.apply(scene)` to theme one explicitly.

## Styling standard controls

```java
Tessel.style(button, ButtonStyle.ACCENT);          // STANDARD, ACCENT, OUTLINE, SUBTLE, DANGER, ICON, LINK
button.setGraphic(new Icon(Symbol.SAVE));          // icons take the button's text color
Label title = Tessel.label("Settings", TextStyle.TITLE);   // DISPLAY, TITLE, SUBTITLE, HEADER, BODY_STRONG, BODY, SECONDARY, CAPTION, CODE
Tessel.textStyle(anyNode, TextStyle.CAPTION);
StackPane search = Tessel.withLeadingIcon(textField, Symbol.SEARCH);   // an icon at the start of a text field
Tessel.progressState(progressBar, ProgressState.PAUSED);   // NORMAL, PAUSED, ERROR, SUCCESS
```

Buttons, toggle buttons and menu buttons take every `ButtonStyle`. Keyboard focus rings only appear after a key was pressed, never after a mouse click. Sliders get an accent-colored fill up to the thumb.

## Components

```java
new Card("Profile", content);                              // setElevated(true) for a shadow
new SettingsCard("Notifications", "Alerts for mentions", Symbol.RINGER, new ToggleSwitch(null, true));
new Expander("Details", content, true);
new InfoBar(Severity.WARNING, "Heads up", "Your subscription expires soon.");
new Badge("New", Badge.Kind.SUCCESS);
new Avatar("Ada Lovelace", 40);
new ProgressRing(32);                                      // setProgress(0.4) for determinate, setState(...) for states
new TextBlock("Wrapping text...", TextStyle.SECONDARY);
new Icon(Symbol.SAVE, 16);                                 // 104 glyphs, see the gallery's Icons page

Snackbar.show(window, "Saved", "Your changes were saved.", Severity.SUCCESS);

ContentDialog dialog = new ContentDialog("Delete file?", "This can't be undone.");
dialog.setPrimaryButtonText("Delete");
dialog.setCloseButtonText("Cancel");
dialog.setPrimaryDestructive(true);
if (dialog.showAndWait(window) == ContentDialog.Result.PRIMARY) { ... }   // or show(window) -> CompletableFuture
```

`ContentDialog`, `Snackbar` and `TeachingTip` are drawn *inside* the window, above its content. A `TesselWindow` has the layer built in; for any other scene it is added the first time it is needed, by wrapping the scene's root.

### SplitButton and DropDownButton

```java
SplitButton save = Tessel.style(new SplitButton("Save", new Icon(Symbol.SAVE)), ButtonStyle.ACCENT);
save.setOnAction(e -> save());                        // the main part
save.addMenuItem("Save as...", () -> saveAs());       // the arrow part; getItems() is the menu

DropDownButton sort = new DropDownButton("Sort by");  // the whole button opens the menu
sort.addMenuItem("Name", () -> sortBy(NAME));
```

They are a `SplitMenuButton` and a `MenuButton`, so everything those support works.

### NumberBox

```java
NumberBox quantity = new NumberBox(1, 0, 99);                       // value, minimum, maximum
quantity.setSuffix(" pcs");
quantity.setSpinButtonPlacement(NumberBox.SpinButtonPlacement.COMPACT);   // INLINE (default), COMPACT, HIDDEN
quantity.valueProperty().addListener((o, was, is) -> use(is.doubleValue()));
```

The value is committed on Enter, on focus loss and when stepped (arrow keys, Page Up / Down, mouse wheel, buttons - hold a button to repeat). It accepts expressions such as `12 * 3 + 4` (`setAcceptsExpression(false)` turns that off), understands both `1.5` and `1,5`, clamps to the range, can wrap around (`setWrapEnabled`), can be empty (`setNullable`; the value is then `NaN`), and flashes red and restores the old value for text it cannot evaluate. `setPrefix`, `setSuffix` and `setFractionDigits` control the display.

### NavigationView

```java
NavigationView nav = new NavigationView("My App");
nav.setPaneHeader(searchField);                           // hidden in the compact rail
nav.addItem("Home", Symbol.HOME, HomePage::new);
nav.addHeader("Section");
nav.addFooterItem("Settings", Symbol.SETTINGS, SettingsPage::new);
nav.addTopBarComponent(themeButton);
nav.select(0);
nav.selectedItemProperty().addListener((o, was, is) -> ...);
```

The pane slides between the 260 px list and the 56 px icon rail with the toggle button, and collapses by itself below `autoCompactWidth` (default 720 px). An accent indicator slides to the selected item and pages fade in.

### TabView

```java
TabView tabs = new TabView();
tabs.addTab("Readme.md", new Icon(Symbol.DOCUMENT, 16), new TextArea());
tabs.setAddTabHandler(() -> tabs.addTab("Untitled", new TextArea()));   // shows the + button
tabs.addTabViewListener(new TabView.TabViewListener() {
    @Override public boolean tabClosing(TabView view, TabView.Tab tab) { return confirmClose(tab); }   // false vetoes
});
```

Drag tabs to reorder (`setReorderable`), middle-click or Ctrl+W to close, Ctrl+PageUp / Ctrl+PageDown to switch. Tabs shrink to fit and then scroll with arrow buttons or the mouse wheel.

### TeachingTip

```java
TeachingTip tip = new TeachingTip("Save your work", "Press Ctrl+S at any time.");
tip.setActionButton("Got it", () -> markTipSeen());
tip.setPlacement(TeachingTip.Placement.AUTO);         // or TOP / BOTTOM / LEFT / RIGHT
tip.show(saveButton);
```

The tip follows its target when the window is resized or scrolled, and closes on Esc or on a click elsewhere (`setLightDismiss(false)` keeps it open). `setTailVisible(false)` turns it into a plain flyout, `setContent(node)` puts any component inside it.

### BreadcrumbBar

```java
BreadcrumbBar bar = new BreadcrumbBar("Home", "Documents", "Reports");
bar.setTruncateOnClick(true);                         // clicking an item makes it the last one
bar.setItemIconProvider(o -> "Home".equals(o) ? new Icon(Symbol.HOME, 14) : null);
bar.addItemClickListener((b, index, item) -> open(index));
```

Items are any objects (`setItemTextProvider` names them). The last item is the current location: bold and not clickable. When the bar is too narrow the first items collapse into a "..." button that lists them in a menu. Keyboard: Left / Right / Home / End move between items, Enter or Space activate, Down opens the "..." menu.

### ColorPickerPane

```java
ColorPickerPane picker = new ColorPickerPane(Color.ORANGE);
picker.setAlphaEnabled(true);                         // adds the opacity slider and field
picker.colorProperty().addListener((o, was, is) -> use(is));
```

A saturation / brightness square, a hue slider, an optional opacity slider, a hex field, RGB(A) number boxes and a row of swatches.

### Windows and dialogs

`TesselWindow` is a `Stage` with its own title bar (icon, title, your components, minimize / maximize / close) in a rounded, shadowed frame. It is dragged by the title bar, resized from its edges and corners, and maximized by double-clicking the title bar. Use `toggleMaximize()` (not `setMaximized`) and `setContentSize` / `setContentMinSize` for sizes - the window is larger than its content by the frame and title bar.

```java
TesselDialog dialog = new TesselDialog(window, "Rename file");   // modal by default; a modality can be passed
dialog.setContent(form);
dialog.showDialog();                                             // packs, centers on the owner, waits until it is closed
```

Esc and the title bar's close button fire a close request first (a `setOnCloseRequest` handler can veto by consuming the event); `dialog.close()` closes without asking. `ContentDialog` remains the choice for confirmations drawn over the window's own content, and a plain `javafx.scene.control.Alert` is themed by the style sheet.

### File chooser

`javafx.stage.FileChooser` opens a native operating system dialog that no theme can change. `TesselFileChooser` has the same
API, so it is a drop-in replacement, but looks like Tessel:

```java
TesselFileChooser chooser = new TesselFileChooser();
chooser.setTitle("Open picture");
chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));   // the standard filter class
File file = chooser.showOpenDialog(window);              // null when cancelled
List<File> files = chooser.showOpenMultipleDialog(window);
File target = chooser.showSaveDialog(window);            // asks before replacing; "report" + filter "*.txt" gives report.txt
File folder = chooser.showDirectoryDialog(window);
```

The dialog has a breadcrumb address bar (click its empty part to type a path), a places sidebar (home, desktop, documents,
downloads, pictures, and the drives with their volume names), back / forward / up / new folder, and list, details and tile
views with sorting. Typing a folder name in the name field opens that folder; in a multi-select dialog several names can be
typed as `"a.txt" "b.txt"`. `FileBrowser` is the pane inside the dialog and can be embedded in any window (see the gallery's
Files page). The chooser texts are English.

### Standard alerts and dialogs

A plain `Alert` or `Dialog` is themed automatically: Tessel's colors, fonts and flat layout, and its vector icon (information,
warning, error, question) in place of the blue and red pictures of the built-in theme. Its window keeps the operating system's
title bar; for Tessel's own title bar use `TesselDialog`, or `ContentDialog` for a confirmation over the window.

## Notes

- **Fonts:** Segoe UI (and Segoe UI Semibold) on Windows, with fallbacks to SF Pro / Helvetica Neue, Inter, Cantarell, Ubuntu, Noto Sans and DejaVu Sans elsewhere. Icons are vector-drawn and need no font.
- **TesselWindow** is pure JavaFX: a transparent stage with a drawn frame and shadow. Unlike Tessel4J's `TesselFrame` it does not hook into the native window procedure, so Windows 11 Snap Layouts and Aero Snap are not available.
- **System theme** follows the operating system live on JavaFX 22+; on JavaFX 21 it is read when the theme is applied - call `Tessel.setTheme(TesselTheme.SYSTEM)` again to pick up a change.
- **Not included (yet):** Tessel4J's `RichTextEditor` / `RichTextToolBar`. JavaFX has no rich text control outside the incubating `jfx.incubator.richtext` module, and TesselFX does not depend on incubating API.
- **Differences from Tessel4J:** `FontIcon` is `Icon` (always vector), `TesselFrame` is `TesselWindow`, `ColorPicker` is `ColorPickerPane` (to avoid clashing with `javafx.scene.control.ColorPicker`), `SplitButton` / `DropDownButton` build on JavaFX's `SplitMenuButton` / `MenuButton`.

### Compatibility

The library compiles against JavaFX 26.0.2 by default and avoids API newer than JavaFX 21, so it runs on JavaFX 21 and
newer. It was compiled against JavaFX 21 and 25 as well, and its behavior was exercised on JavaFX 21.0.11.

## License

MIT - see [LICENSE](LICENSE).
