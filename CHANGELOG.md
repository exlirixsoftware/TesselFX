# Changelog

All notable changes to TesselFX are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/): new components and features raise the minor version, and changes
that break existing code would raise the major version.

| Version | Date       | Summary |
| ------- | ---------- | ------- |
| [1.0.0](#100---2026-10-12) | 2026-10-12 | First release: themeable style sheet for every standard JavaFX control, the core Tessel components, a window with a Tessel title bar, and the gallery. |

---

## [1.0.0] - 2026-10-12

The JavaFX port of the Tessel design system (Tessel.NET, Tessel4J, Tessel.js): the same palette, radii, spacing and
component set.

### Added

#### Theme engine (`net.tessel.fx`)

- **`Tessel`**: `setup`, `setTheme`, `setAccent`, `useSystemAccent`, `palette`, `paletteProperty`, theme listeners.
  Light, dark and system themes; the system theme is followed live on JavaFX 22+.
- Every scene of every window - including pop-ups such as context menus, combo box lists and tool tips - is themed
  automatically and follows runtime theme and accent changes. The palette is generated as `-tsl-*` looked-up colors
  and delivered as a `data:` style sheet, so no files are written.
- **`Palette`** with the Tessel design tokens, custom accent derivation (hover, pressed, subtle and an accessible
  `on-accent` color), **`ButtonStyle`**, **`TextStyle`**, **`Severity`**, **`ProgressState`**, **`Fonts`**.
- Helpers: `Tessel.style`, `Tessel.textStyle`, `Tessel.label`, `Tessel.progressState`, `Tessel.withLeadingIcon`.
- Keyboard focus rings that appear only after a key was pressed.

#### Standard controls with a Tessel look (style sheet, no code needed)

Button, ToggleButton, Hyperlink, MenuButton, SplitMenuButton, CheckBox, RadioButton, TextField, PasswordField,
TextArea, ComboBox, ChoiceBox, Spinner, Slider (with an accent-filled track), ProgressBar, ProgressIndicator,
ScrollPane, ScrollBar, ListView, TableView, TreeView, TabPane, MenuBar, Menu, ContextMenu, MenuItem and its check and
radio variants, Separator, Tooltip, ToolBar, SplitPane, TitledPane, Accordion, Pagination, DatePicker, ColorPicker,
DialogPane (`Alert`, `Dialog`).

#### Components (`net.tessel.fx.components`)

- **`TesselWindow`**: title bar with icon, title, custom components and window buttons; rounded frame with shadow;
  drag, edge and corner resize, maximize and restore. **`TesselDialog`**: the same chrome for modal dialogs.
- **`NavigationView`**: lazy pages, expandable pane / icon rail, automatic compact mode, animated accent indicator and
  page transitions, header and footer items, pane header, top bar components.
- **`TabView`**: closable, reorderable tabs with an add button, shrink-to-fit with scrolling, listener with veto,
  keyboard shortcuts.
- **`ContentDialog`**: in-window modal dialog with backdrop, primary / secondary / close buttons, destructive style,
  validation hook, `show` (future) and `showAndWait`.
- **`TeachingTip`**: popover with a tail that follows its target; placement, light dismiss, action button.
- **`Snackbar`**, **`InfoBar`**, **`Card`**, **`SettingsCard`**, **`Expander`**, **`BreadcrumbBar`**, **`SplitButton`**,
  **`DropDownButton`**, **`NumberBox`** (expressions, range, wrap, empty value, prefix / suffix), **`ColorPickerPane`**,
  **`ToggleSwitch`**, **`ProgressRing`** (determinate, indeterminate, states), **`Badge`**, **`Avatar`**,
  **`TextBlock`**.

- **`TesselFileChooser`** and **`FileBrowser`**: a themed replacement for `javafx.stage.FileChooser` and
  `DirectoryChooser` with open, open-multiple, save and folder modes, extension filters, breadcrumb address bar with
  typed paths, places sidebar with drive names, history, new folder, list / details / tile views, sorting, hidden files,
  default extension and overwrite confirmation.
- Standard **`Alert`** and **`Dialog`** windows: sized with Tessel's fonts from the first layout (the buttons of
  German or other long-text dialogs were cut off), flat layout without corner artifacts, and a vector icon instead of the
  blue and red Modena pictures.

#### Icons (`net.tessel.fx.icons`)

- **`Icon`** and **`Symbol`**: 104 vector glyphs (the Tessel.js icon table) that follow the text color of their
  container, with no icon font.

#### Build and gallery

- A Maven build (parent POM, `tessel-fx` library module, `tessel-gallery` demo) with JavaFX from Maven Central
  (`javafx.version`, 26.0.2 by default); `mvn -pl tessel-gallery javafx:run` starts the gallery.
- The library's class files target Java 17, so it runs on JDK 17+ with JavaFX 21 or newer; the gallery targets Java 26.
- **Gallery** with 15 pages: Home, Buttons, Inputs, Selection, Collections, Menus, Feedback, Dialogs, Pickers, Files,
  Tabs, Layout, Palette, Icons and Settings. `--screenshots=<dir>` writes a PNG of every page in both themes.

### Known limitations

- `TesselWindow` is pure JavaFX: no Windows 11 Snap Layouts or Aero Snap.
- No rich text editor yet (JavaFX only offers one in the incubating `jfx.incubator.richtext` module).
