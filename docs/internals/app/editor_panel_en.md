# EditorPanel

> 한국어: [editor_panel.md](https://monkshark.github.io/page-ide/#internals/app/editor_panel.md)

> `page/app/src/main/kotlin/page/app/EditorPanel.kt` — Editor body

A single `BasicTextField` with a line-number gutter, token coloring, match highlights, bracket match, current-line background, and a status bar layered on top.

---

## Signature

```kotlin
@Composable
fun EditorPanel(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    activePath: Path?,
    lexer: SyntaxLexer?,
    search: SearchState?,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    pushHistory: (EditSnapshot) -> Unit,
    modifier: Modifier = Modifier,
)
```

| Parameter | Meaning |
|---|---|
| `value` | Text + selection — vanilla Compose `TextFieldValue` |
| `lexer` | `null` ⇒ no token coloring (body tone only) |
| `search` | Drives match / active-match highlights |
| `pushHistory(prev)` | Pushes a pre-edit snapshot — actual undo/redo dispatch lives in `Main`'s `doUndo` / `doRedo` |

---

## TextBuffer & tokens

```kotlin
val buffer = remember(value.text) { TextBuffer(value.text) }
val tokens = remember(value.text, lexer) { lexer?.tokenize(value.text).orEmpty() }
```

`buffer` and `tokens` are both keyed on `value.text` — recomputed only when text changes. The line-number gutter, status bar, and current-line highlight all read from the same `buffer`.

---

## Bracket match

```kotlin
val bracketMatch = remember(value.text, value.selection.start, value.selection.end) {
    if (value.selection.collapsed) BracketMatch.find(value.text, value.selection.start) else null
}
```

Only matches when the selection is collapsed (i.e., a single caret). During a selection it stays `null` to avoid visual noise.

---

## Current-line background

```kotlin
val currentLineBg = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
.drawBehind { ... }
```

A thin rectangle at the y range of `buffer.lineColOf(caret).line`. Painted with `drawBehind` so the color sits under the glyphs.

---

## Scroll lock after focus

```kotlin
when focusGainVersion ↑, LaunchedEffect(focusGainVersion) {
    delay(250)
    scrollState.scrollTo(scrollState.value, MutatePriority.PreventUserInput)
}
```

Right after a tab switch, `BasicTextField` likes to drag the caret into view. We pin the scroll position once during the first 250ms to ignore that. `MutatePriority.PreventUserInput` means real user scroll still wins.

---

## `onValueChange` chain

```kotlin
val after = AutoClose.apply(value, next)
val unindented = Indent.maybeUnindentClosingBrace(after)
val final = Indent.maybeApplyEnter(unindented)
onValueChange(final)
```

Each keystroke runs through three post-processors: `AutoClose` for matched brackets/quotes, `Indent` for closing-brace alignment and Enter-driven auto-indent.

---

## Key handling (`onPreviewKeyEvent`)

| Key | Action |
|---|---|
| `Alt+Up/Down` | `LineMove.moveUp/moveDown` |
| `Alt+Shift+Up/Down` | `duplicateUp/duplicateDown` |
| `Tab` | Inside a Markdown fence ⇒ `handleLiteralTab`; otherwise indent |
| `Shift+Tab` | `handleShiftTab` (outdent) |
| `Enter` | `handleEnter` (auto-indent) |
| `Backspace` | `handleBackspace`; if it returns `null`, default behavior runs |

`MarkdownFence.isInsideFence` decides the `Tab` branch — code blocks need a literal tab so the WYSIWYG output stays intact.

---

## Mouse clicks (double / triple)

```kotlin
.pointerInput(Unit) {
    awaitPointerEventScope {
        ... only Press events on PointerEventPass.Final
        clickCount = if (now - lastClickTime < 400 && close && clickCount < 3) clickCount + 1 else 1
    }
}
```

`onTextLayout` captures the `TextLayoutResult`; `getOffsetForPosition` converts the click coordinates to a text offset.

| Click | Action |
|---|---|
| Double | `WordBoundary.wordRangeAt(text, offset)` — selects the same-class run (no-op on whitespace / newline) |
| Triple | `WordBoundary.lineRangeAt(text, offset)` — line start to just before `\n` |

`PointerEventPass.Final` runs after `BasicTextField`'s built-in pointer logic, so we override its result. The sequence only counts within 400 ms and 8 px; outside that the counter resets.
---

## Ctrl+Click navigation

| Cursor sits on | Action |
|---|---|
| A reference | Jump to the declaration (`onRequestDefinition` → `onGoToDefinition`) |
| The declaration | Show usages in a popup beside the caret (`ReferencesSurface.Popup`) |

Holding `Ctrl` over an identifier underlines it, switches to the hand cursor, and shows which way the click will go (`Go to declaration` / `Find usages`). Whether the word is a declaration is decided locally from `SymbolNames.scan(text).defs` — no LSP round trip.

`Ctrl+B` runs the same path (`triggerDefinitionOrReferences`). `Shift+F12` still fills the bottom References panel, and the popup's `Open in panel` moves a result set down there.

`ReferencesPopup` groups hits by file and takes `↑↓` to move, `Enter` to open, `Esc` to close.
Two waits are visible — while the LSP definition request is in flight a `Resolving declaration of <symbol>…` label sits beside the caret (it clears itself after 8 s), and while the workspace index is still building the popup carries an `Indexing — list may grow` note so a short list is not mistaken for a complete one.
---

## Action catalog

Shortcuts, labels, and groups live only in `page.app.input.ActionCatalog`. One row is one action, and key dispatch, the context menu, and the action palette all read that same row.

| Surface | Reads |
|---|---|
| `ActionDispatcher` | binding → run (replaces `ShortcutResolver`) |
| Context menu | label plus its shortcut |
| Action palette (`Ctrl+Shift+P`) | every action, searchable |

`Binding.primary` resolves to `Ctrl` on Windows and Linux, `Cmd` on macOS. Chords that must stay literal `Ctrl` on a Mac (`Ctrl+Alt+T` and friends) set `control = true` instead. `ShortcutLabels` renders the same binding as `Ctrl+Shift+F` or `⇧⌘F` depending on the platform.
Each row carries a `context` saying where the key is heard — `Global` rows run straight from `ActionDispatcher`, while `Editor` and `FileTree` rows are handled by those screens alongside their own state (completion popup, snippet tabstops, tree selection). Those rows have no `run`, so nothing dispatches them, but the catalog still owns their label and binding, which is what lets the palette, the context menus, and the platform labels read one table.

Key matching itself now goes through `isPrimaryPressed()`: `Ctrl` on Windows and Linux, `Cmd` on macOS — including the editor's `Ctrl+A/C/X/V`, word motion, `Ctrl+Click`, and the file tree's copy and paste.
The **Keymap** screen in Settings and the macOS menu bar read the same table. The sheet lists everything under `Anywhere` / `While editing` / `In the file tree`; the menu bar takes only `Global` rows that have a body and groups them into File · Edit · Navigate · Code · View · PAGE. It renders on macOS only, and `apple.laf.useScreenMenuBar` puts it in the system bar at the top of the screen.
On Windows and Linux the same table also becomes the title-bar menu. `File Edit Navigate Code View PAGE` take the place of the `PAGE` wordmark (the mark stays), and below 900 dp of window width they collapse into a single `Menu` button. macOS keeps the wordmark because its menus live in the system bar instead. All three surfaces read the same grouping from `ActionMenus`.

---

## Code folding

```kotlin
val foldRegions = remember(value.text) { FoldRegions.detect(value.text) }
var foldedRegions by remember(activePath) { mutableStateOf(emptySet<FoldRegions.Region>()) }
val foldSegments = FoldRegions.segmentsFor(value.text, activeFolds)
```

Brace-pair folding. The gutter shows ▾/▸ toggles; clicking updates the `foldedRegions` set. When the text changes, `foldRegions` is recomputed and any folded region that no longer exists drops out automatically (`activeFolds` filter).

A folded region renders as `{ ... }` with only the `...` substring tinted gray and softly backgrounded. Only clicks on `...` unfold — `{`, the surrounding spaces, and `}` stay regular text so drag-selection / copy-paste shortcuts work over the placeholder. The `pointerInput` Press handler hands the transformed offset to `FoldRegions.foldedRegionAt`, which only matches inside the `...` window, and consumes the event on a hit.

`gutterLines` excludes every line in `(startLine+1..endLine)` for an active fold — lines hidden from the body are hidden from the gutter as well.

Tab switches (`activePath` change) reset the fold state — keyed via `remember(activePath)`.

---

## `CombinedHighlightTransformation`

A single `VisualTransformation` handles four things:

1. Token colors (`colorFor(kind)`; `PUNCT` returns `null` to keep body color).
2. Match backgrounds — active match vs. regular match.
3. Bracket-match background.
4. Folding — when `foldSegments` is non-empty, the body gets spliced with ` ... ` placeholders and a `FoldOffsetMapping` is used.

Without folds it returns `OffsetMapping.Identity`. With folds it returns a thin mapping built on top of `FoldRegions.{originalToTransformed, transformedToOriginal}`.

---

## `EditorStatusBar`

A thin bottom row: `Ln {line+1}, Col {col+1}` · line count · char count. The numbers come straight from `buffer.lineColOf(caret)`.

---

## Font

| Property | Value |
|---|---|
| Family | `EditorFontFamily` |
| Size | `14sp` |
| Line height | `20sp` |
| Line alignment | `LineHeightStyle(Center, Trim.None)` |

`Trim.None` keeps the same line height on the first/last line — perfect alignment with the gutter.

---

## Usage

| Location | Purpose |
|---|---|
| `page.app.Main`'s body slot | Pipes the active tab's text / lexer / search through |

---

- [Back to index](https://monkshark.github.io/page-ide/#README_en.md)

---

## Unused diagnostics and quick fixes

What `UnusedSymbols` finds no longer stops at grey text. `UnusedQuickFixes.diagnostics` turns each range into a WARNING and publishes it through `LocalDiagnostics`, which the Problems panel merges with the LSP's own (`Unused import` / `'name' is never used`). It works with no language server running.

`Alt+Enter` prepends local fixes to whatever the LSP offers — `Remove unused import` for the line under the caret, and `Remove all unused imports (N)` for the file. The bulk edit deletes from the bottom up so earlier line numbers stay valid. Declarations only get the warning: deleting one can change what the code means.

Context menus are drawn by one component, `CompactMenuItem`: a 14 dp Material Outlined icon column, an inset rounded hover, a right-aligned shortcut column, and `danger = true` tinting only the label (a fully red row reads as an error state). `CompactMenuSeparator` breaks the groups apart. The title-bar menus, the tab menu, and the run dropdown share the component, so they all move together.

`Alt+Enter` opens beside the caret. Local fixes (dead imports and friends) are already computed, so they paint **immediately** while the header shows `searching…`; the server's actions merge in when they arrive, so there is no longer a stretch where nothing is on screen.

The selected fix shows its diff inside the popup, up to six lines, then folds into `+N more lines`. A fix that reaches other files lists their names and edit counts. Each row is tagged with its kind (quick fix / refactor / source). Pressing `Alt+Enter` again applies the selected fix.

A fix that reaches other files moves from the popup's `Open in panel` into the bottom-dock **review surface** (`CodeActionReviewPanel`). It sits where Problems and Todo already live, so it gets the full window width: the files the edit touches on the left with their edit counts, the selected file's diff on the right.

Nothing is chosen here — the action is already picked, and this is where it gets read and accepted. Hence the header's `N edits in M files` and the explicit `Apply` / `Cancel · Esc`. Unticking a file drops all of its edits (`filterEdit`) and turns the header warning-coloured on purpose: a half-applied rename does not compile.
