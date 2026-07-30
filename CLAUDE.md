# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Magellan is a Swing desktop application: a map / character / sprite editor for the TI-99/4A home computer
(fork of Howard Kistler's original, extended by the AtariAge TI-99/4A community). It edits `.mag` projects
and exports them as XB/BASIC, assembly, CVBasic, binary, and image data for real TI hardware and emulators.

Plain Java SE + Swing. No third-party dependencies, no build system checked in, no test suite.

## Build & run

The canonical build is the IntelliJ artifact `Magellan:jar` (`.idea/artifacts/Magellan_jar.xml`,
`build-on-make="true"`), which compiles `src/` to `out/production/Magellan` and writes `dist/Magellan.jar`
using `META-INF/MANIFEST.MF` (`Main-Class: com.dreamcodex.ti.Magellan`). Both `.idea/` and `META-INF/` are
gitignored, so a fresh clone has neither — recreate the manifest or build from the command line:

```bash
# compile (verified with JDK 21; installer targets Java 1.8+)
find src -name "*.java" > /tmp/sources.txt
javac -nowarn -d out/production/Magellan @/tmp/sources.txt

# icons/splash are loaded via getClass().getResource(...) and must be inside the jar
(cd src && find . -name "*.png" -exec cp --parents {} ../out/production/Magellan/ \;)

jar cfe dist/Magellan.jar com.dreamcodex.ti.Magellan -C out/production/Magellan .
```

Run: `java -jar dist/Magellan.jar [file.mag]` — the single optional argument is a project file opened at
startup. `dist/Magellan.sh` is the shipped launcher; `dist/Magellan.install4j` builds the binary installers.

There are no tests, no linter, and no CI. Verification is manual: build the jar, launch it, and exercise the
affected editor/import/export path. `dist/examples/*.mag` and `dist/default.mag` are useful sample projects.

## Architecture

### The four objects passed everywhere

Almost every class takes the same quartet in its constructor:

- `Magellan` (the `JFrame`, referred to as `parent`) — application shell, active char/sprite selection,
  char/sprite button icon regeneration, color-mode switching, dialogs.
- `MagellanUI` (`ui/`) — builds the entire menu bar and main panel; owns the Swing widgets. `Magellan`
  reaches into it via getters (`getCharGridCanvas()`, `getCharButtons()`, …).
- `DataSet` (`util/`) — the project's graphics data: `charGrids`/`charColors`/`charImages`/`charNames`/
  `charProperties`, `spriteGrids`/`spriteColors`/`spriteImages`, `clrSets`, the ECM palette arrays, and the
  current `ColorMode`. Pure state holder, all getters/setters.
- `Preferences` (`util/`) — user + export settings, persisted as a `Properties` file at
  `~/Magellan.prefs` (`System.getProperty("user.home")`). Also holds the recent-files list and the last-used
  export options (character/sprite ranges, compression, transition type, offset).

Map data lives separately in `MapEditor`, which is passed alongside the quartet.

### Action layer (`actions/`)

`MagellanAction extends AbstractAction` is the base; every menu item, toolbar button, and keyboard binding is
a subclass, constructed inline in `MagellanUI.createMenu()` / `createMainPanel()`. Two specializations:

- `EditorAction` — `final actionPerformed` that calls `performAction(e)`, then `mapEditor.redrawCanvas()`,
  catching exceptions into an error dialog. Subclasses implement `performAction`.
- `FileAction` — adds `getFileFromChooser(...)` plus the file-extension constants (`mag`, `xb`, `a99`, `mgb`,
  `vdp`, …). Export actions follow a fixed shape: show a `MagellanExportDialog`, copy the chosen options back
  into `Preferences`, pick a file, then delegate to an `Exporter`.

`Magellan.actionPerformed` is *not* the dispatcher any more — it is a residual router for the handful of
components that still fire raw string commands (`DualClickButton`, `TripleClickButton`, `ECMPaletteComboBox`
in the color docks). **To add a command, write an Action subclass and wire it in `MagellanUI`**; don't extend
the string-command switch. The `Globals.CMD_*` constants are legacy and only partly live.

### Canvases and the aliasing rule

- `GridCanvas` (`component/`) — the 8×8 character and 16×16 sprite editors. Holds `gridData` (color indices
  per pixel) and, in bitmap mode, `gridColors[row][0=back, 1=fore]`. Has its own undo/redo (`ArrayList<int[][][]>`),
  independent of the map's.
- `MapCanvas` — map rendering and interaction: char layer + sprite layer, look/clone/flood-fill modes, text
  cursor, grid, overlay image, sprites-per-line warning colors.
- `MapEditor` — the panel wrapping `MapCanvas` with all the map toolbars. Owns the multi-map project state:
  `ArrayList<int[][]> arMaps`, `ArrayList<Integer> arClrs` (per-map screen color), and
  `ArrayList<HashMap<Point, ArrayList<Integer>>> spriteMaps` (sprite placements in *pixel* coordinates, with a
  list of sprites per point). Map undo uses `NotifyingUndoManager` + `MapAction` edits — a different mechanism
  from `GridCanvas`'s.

**Important:** `GridCanvas.setGridAndColors(gd, gc)` assigns the arrays by reference, and `EditCharAction`
stores `charCanvas.getGridData()` straight into `DataSet.charGrids`. The editor canvas and the stored
character/sprite are the *same array object* — drawing mutates the project data in place, and cloning
(`Globals.cloneGrid`) is required wherever a snapshot is intended.

### Color modes

`ColorMode` (Graphics 1 / Bitmap / ECM-2 / ECM-3) is the single biggest branch in the codebase; nearly every
render, import, export, and file-format path switches on it.

- **Graphics 1** — one fore/back pair per 8-character color set: `clrSets[charNum / 8][INDEX_CLR_FORE|BACK]`.
- **Bitmap** — a fore/back pair per pixel row: `charColors.get(ch)[row][0|1]`.
- **ECM 2/3** (F18A) — 2 or 3 bitplanes, so grid values are palette indices 0–3 / 0–7 into a per-character
  `ECMPalette`. Palettes are shared mutable objects: `ecmCharPalettes[ch]` / `ecmSpritePalettes[s]` point into
  `ecmPalettes`, and code identifies a palette by *object identity* (`getECMPaletteIndex`, the `paletteMap` in
  exporters). Don't copy `ECMPalette` instances casually.

`Magellan.setGraphicsColorMode()` / `setBitmapColorMode()` / `setECM2ColorMode()` / `setECM3ColorMode()` perform
lossy conversions when switching (`limitCharGrids`, `limitSpriteGrids`, nearest-color matching) and rebuild the
color docks.

### Ranges

`TIGlobals` defines the hardware limits: characters 0–2047, sprites 0–511, `COLOR_SET_SIZE = 8`. The active
range depends on the character-set size option (`Magellan.CHARACTER_SET_BASIC` 32–159 / `EXPANDED` 0–255 /
`SUPER` 0–2047, with matching sprite limits) — use `Magellan.getCharacterSetStart/End/Size(...)` and
`getSpriteSetEnd/Size(...)` rather than hardcoding.

### Import / export

`importers/` and `exporters/` are parallel hierarchies. `Importer` and `Exporter` each have a fat constructor
that flattens `DataSet` + `Preferences` into protected fields (`charGrids`, `clrSets`, `ecmCharPalettes`,
`colorMode`, `offset`, …), so subclasses work with those fields, not with `DataSet` itself.

- **`.mag` project files** — line-oriented text, one record per line, each prefixed with a `Globals.KEY_*`
  tag (`KEY_CHARDATA`, `KEY_MAPDATA`, `KEY_SPRITE_PATTERN`, …) and interleaved with `* SECTION` comments.
  `DataFileExporter` / `DataFileImporter` are the pair; sections are conditional on color mode (extra bitplane
  sections for ECM, `CHAR COLORS` for bitmap). Adding a field means touching both sides and keeping older
  files readable.
- **Binary** — `Exporter.BIN_HEADER_MAG` (`"MG"`) + `BIN_HEADER_VER` (`"03"`) and `BIN_CHUNK_*` flags;
  format documented in `dist/magellan_binary.txt`.
- **Scroll data** (`ScrollFileExporter`, the largest exporter) generates character sets for smooth scrolling:
  `TransitionType` (top-to-bottom, left-to-right, two-dimensional, isometric …) drives `TransChar`, which
  records for each source character the characters it must transition into, plus `MetaTile` for 8×8 metatiles.
  `AnalyzeCharTransDialog` is the interactive view of the same data.
- Export options come from `MagellanExportDialog` (`TYPE_BASIC`, `TYPE_ASM`, `TYPE_BINARY`, `TYPE_XBSCRMER`,
  `TYPE_SCROLL`, `TYPE_BINARY_MAP`), which also owns the `COMPRESSION_*` constants.

## Conventions

- Icons/images live under `src/com/dreamcodex/ti/images/` and `src/com/dreamcodex/ti/component/images/`,
  loaded by classloader-relative name — keep them inside the source tree so they land in the jar.
- Grids are `int[][]` indexed `[y][x]`; a `HashMap<Integer, int[][]>` keyed by char/sprite number is the
  standard container, and a `null` value means "undefined character" (rendered as a letter or `?`).
- `Globals` holds shared UI colors/borders and the grid/hex helper functions (`getHexString`,
  `getSpriteHexString`, `cloneGrid`, `getClosestColorIndex`, …).
- Bump `Magellan.VERSION_NUMBER` and add a section to `dist/release changes.txt` for a release; the version in
  `dist/Magellan.install4j` is maintained separately.
- The code targets an old language level (`.idea/misc.xml` says JDK 1.6, install4j requires 1.8+) and uses
  deprecated Swing APIs (`ActionEvent.SHIFT_MASK`, `KeyEvent.CTRL_MASK`); it compiles with warnings on modern
  JDKs. Match the surrounding style rather than modernizing opportunistically.
