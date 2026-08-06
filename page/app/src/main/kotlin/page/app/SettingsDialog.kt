package page.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.lsp.LspBackends
import page.shared.syntax.SyntaxPreset
import page.ui.Glass
import page.ui.GlassPalette
import page.ui.glassTokensFor

data class PageSettings(
    val autoSave: AutoSaveOptions = AutoSaveOptions.DEFAULT,
    val editor: EditorOptions = EditorOptions.DEFAULT,
    val lsp: LspOptions = LspOptions.DEFAULT,
    val autoInput: AutoInputOptions = AutoInputOptions.DEFAULT,
    val ui: UiOptions = UiOptions.DEFAULT,
    val run: RunOptions = RunOptions.DEFAULT,
)

val LocalPageSettings = staticCompositionLocalOf { PageSettings() }

private val CenteredLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private enum class SettingsCategory(val label: String, val group: String, val icon: ImageVector) {
    AUTO_SAVE("AutoSave", "Workspace", Icons.Outlined.Save),
    EDITOR("Editor", "Workspace", Icons.AutoMirrored.Outlined.Notes),
    LSP("LSP", "Workspace", Icons.Outlined.Extension),
    AUTO_INPUT("AutoInput", "Workspace", Icons.Outlined.Code),
    UI("UI", "Appearance", Icons.Outlined.Palette),
    RUN("Run", "Tasks", Icons.Outlined.PlayArrow),
    KEYMAP("Keymap", "Appearance", Icons.Outlined.KeyboardAlt),
}

@Composable
internal fun SettingsPanel(
    settings: PageSettings,
    onApply: (PageSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opened = remember { settings }
    var selected by remember { mutableStateOf(SettingsCategory.AUTO_SAVE) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val colors = Glass.colors
    val revert = { if (settings != opened) onApply(opened) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Glass.radius.lg))
            .background(colors.surface)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false
                else when (event.key) {
                    Key.Escape, Key.Enter, Key.NumPadEnter -> { onClose(); true }
                    else -> false
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Settings",
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = Glass.type.title,
            )
            Spacer(Modifier.weight(1f))
            CloseButton(onClose)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SettingsSidebar(
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.width(168.dp).fillMaxHeight(),
            )
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(colors.separator))
            SettingsDetailPane(
                category = selected,
                draft = settings,
                onChange = onApply,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (settings != opened) {
                Text(
                    text = "Changes apply as you edit",
                    color = colors.muted,
                    fontSize = Glass.type.label,
                    style = TextStyle(lineHeight = Glass.type.label, lineHeightStyle = CenteredLineHeight),
                )
                Spacer(Modifier.width(12.dp))
                GlassButton(label = "Revert", primary = false, onClick = { revert() })
                Spacer(Modifier.width(8.dp))
            }
            GlassButton(label = "Done", primary = true, onClick = onClose)
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(Glass.radius.sm))
            .background(if (hovered) colors.surfaceL2 else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "✕", color = if (hovered) colors.text else colors.muted, fontSize = Glass.type.body)
    }
}

@Composable
private fun SettingsSidebar(
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(Glass.colors.surfaceL1).padding(8.dp)) {
        var lastGroup: String? = null
        for (cat in SettingsCategory.values()) {
            if (cat.group != lastGroup) {
                GroupLabel(cat.group, first = lastGroup == null)
                lastGroup = cat.group
            }
            SidebarRow(cat = cat, selected = cat == selected, onClick = { onSelect(cat) })
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun GroupLabel(text: String, first: Boolean) {
    Text(
        text = text.uppercase(),
        color = Glass.colors.faint,
        fontSize = Glass.type.label,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(start = 10.dp, top = if (first) 4.dp else 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun SidebarRow(cat: SettingsCategory, selected: Boolean, onClick: () -> Unit) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> colors.primarySoft
            hovered -> colors.surfaceRaised
            else -> colors.surfaceRaised.copy(alpha = 0f)
        },
        animationSpec = tween(120),
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.muted,
        animationSpec = tween(120),
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .size(width = 2.5.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Glass.radius.sm))
                .background(bg)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = cat.icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = cat.label,
                color = fg,
                fontSize = Glass.type.ui,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                style = TextStyle(lineHeight = Glass.type.ui, lineHeightStyle = CenteredLineHeight),
            )
        }
    }
}

@Composable
private fun SettingsDetailPane(
    category: SettingsCategory,
    draft: PageSettings,
    onChange: (PageSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = category.label,
            color = Glass.colors.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = Glass.type.title,
        )
        Spacer(Modifier.height(14.dp))
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll)) {
            when (category) {
                SettingsCategory.AUTO_SAVE -> AutoSaveSection(draft.autoSave) { onChange(draft.copy(autoSave = it)) }
                SettingsCategory.EDITOR -> EditorSection(draft.editor) { onChange(draft.copy(editor = it)) }
                SettingsCategory.LSP -> LspSection(draft.lsp) { onChange(draft.copy(lsp = it)) }
                SettingsCategory.AUTO_INPUT -> AutoInputSection(draft.autoInput) { onChange(draft.copy(autoInput = it)) }
                SettingsCategory.UI -> UiSection(
                    o = draft.ui,
                    e = draft.editor,
                    onChange = { onChange(draft.copy(ui = it)) },
                    onEditorChange = { onChange(draft.copy(editor = it)) },
                )
                SettingsCategory.RUN -> RunSection(draft.run) { onChange(draft.copy(run = it)) }
                SettingsCategory.KEYMAP -> KeymapSection()
            }
        }
    }
}

@Composable
private fun AutoSaveSection(o: AutoSaveOptions, onChange: (AutoSaveOptions) -> Unit) {
    CheckRow(
        label = "Save on window deactivation",
        description = "Save all dirty tabs when PAGE loses focus.",
        checked = o.onFocusLost,
        onToggle = { onChange(o.copy(onFocusLost = !o.onFocusLost)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Save while idle",
        description = "Save after N seconds of no input. 0 = disabled.",
        checked = o.idleSeconds > 0,
        onToggle = { onChange(o.copy(idleSeconds = if (o.idleSeconds > 0) 0 else 15)) },
    )
    Spacer(Modifier.height(8.dp))
    NumberField(
        label = "Seconds",
        value = o.idleSeconds,
        min = 0,
        max = 3600,
        onChange = { v -> onChange(o.copy(idleSeconds = v)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Save before Run",
        description = "Always flush to disk before running.",
        checked = o.beforeRun,
        onToggle = { onChange(o.copy(beforeRun = !o.beforeRun)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Save on tab close",
        description = "Save a dirty tab silently when closing instead of prompting.",
        checked = o.onClose,
        onToggle = { onChange(o.copy(onClose = !o.onClose)) },
    )
}

@Composable
private fun EditorSection(o: EditorOptions, onChange: (EditorOptions) -> Unit) {
    NumberField(
        label = "Tab size",
        value = o.tabSize,
        min = 1,
        max = 16,
        onChange = { v -> onChange(o.copy(tabSize = v)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Insert spaces for Tab",
        description = "Use space characters when Tab is pressed (off = literal \t).",
        checked = o.useSpacesForTab,
        onToggle = { onChange(o.copy(useSpacesForTab = !o.useSpacesForTab)) },
    )
}

@Composable
private fun LspSection(o: LspOptions, onChange: (LspOptions) -> Unit) {
    CheckRow(
        label = "Show inlay hints",
        description = "Inline hints from the LSP (parameter names, types).",
        checked = o.showInlayHints,
        onToggle = { onChange(o.copy(showInlayHints = !o.showInlayHints)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Trigger completion mid-word",
        description = "Show completion menu when typing inside an existing word.",
        checked = o.triggerCompletionMidWord,
        onToggle = { onChange(o.copy(triggerCompletionMidWord = !o.triggerCompletionMidWord)) },
    )
    Spacer(Modifier.height(8.dp))
    NumberField(
        label = "Hover delay (ms)",
        value = o.hoverDelayMs,
        min = 0,
        max = 5000,
        onChange = { v -> onChange(o.copy(hoverDelayMs = v)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Inline diagnostics",
        description = "Draw errors/warnings directly in the editor (Problems panel stays available).",
        checked = o.showInlineDiagnostics,
        onToggle = { onChange(o.copy(showInlineDiagnostics = !o.showInlineDiagnostics)) },
    )
    Spacer(Modifier.height(18.dp))
    SectionLabel("Problems panel scope")
    Spacer(Modifier.height(3.dp))
    Hint("Which files' errors/warnings the Problems panel lists. Inline squiggles always follow the focused file.")
    Spacer(Modifier.height(8.dp))
    Row {
        ChoiceChip(
            label = "Current file",
            selected = o.diagnosticsScope == DiagnosticsScope.CURRENT_FILE,
            onClick = { onChange(o.copy(diagnosticsScope = DiagnosticsScope.CURRENT_FILE)) },
        )
        Spacer(Modifier.width(6.dp))
        ChoiceChip(
            label = "Open tabs",
            selected = o.diagnosticsScope == DiagnosticsScope.OPEN_TABS,
            onClick = { onChange(o.copy(diagnosticsScope = DiagnosticsScope.OPEN_TABS)) },
        )
    }
    Spacer(Modifier.height(18.dp))
    SectionLabel("Server executable overrides")
    Spacer(Modifier.height(3.dp))
    Hint("Point PAGE at a specific LSP binary. Empty = auto-detect (PAGE installer → PATH).")
    Spacer(Modifier.height(8.dp))
    val backends = remember { LspBackends.all().sortedBy { it.displayName } }
    for (backend in backends) {
        PathField(
            label = backend.displayName,
            value = o.serverPaths[backend.id].orEmpty(),
            onChange = { v ->
                val next = o.serverPaths.toMutableMap()
                if (v.isBlank()) next.remove(backend.id) else next[backend.id] = v
                onChange(o.copy(serverPaths = next))
            },
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun AutoInputSection(o: AutoInputOptions, onChange: (AutoInputOptions) -> Unit) {
    CheckRow(
        label = "Auto-pair brackets and quotes",
        description = "Insert the matching closer for ( [ { \" '.",
        checked = o.pairs,
        onToggle = { onChange(o.copy(pairs = !o.pairs)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Auto-close HTML tags",
        description = "In .html/.htm, typing <div> inserts </div>.",
        checked = o.htmlTags,
        onToggle = { onChange(o.copy(htmlTags = !o.htmlTags)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Backspace deletes empty pair",
        description = "Backspace inside (|) removes both characters.",
        checked = o.backspaceDeletesPair,
        onToggle = { onChange(o.copy(backspaceDeletesPair = !o.backspaceDeletesPair)) },
    )
}

@Composable
private fun UiSection(
    o: UiOptions,
    e: EditorOptions,
    onChange: (UiOptions) -> Unit,
    onEditorChange: (EditorOptions) -> Unit,
) {
    SectionLabel("Glass palette")
    Spacer(Modifier.height(3.dp))
    Hint("The token set that colors every surface, syntax, and control.")
    Spacer(Modifier.height(10.dp))
    val palettes = GlassPalette.values().toList()
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        for (rowPalettes in palettes.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                for (p in rowPalettes) {
                    PaletteSwatch(
                        palette = p,
                        selected = p == o.palette,
                        onClick = { onChange(o.copy(palette = p)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowPalettes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    NumberField(
        label = "Sidebar width (dp)",
        value = o.sidebarWidth,
        min = 120,
        max = 800,
        onChange = { v -> onChange(o.copy(sidebarWidth = v)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Show tab close button",
        description = "Display the × button on tabs.",
        checked = o.showTabCloseButton,
        onToggle = { onChange(o.copy(showTabCloseButton = !o.showTabCloseButton)) },
    )

    Spacer(Modifier.height(20.dp))
    SectionLabel("Editor surface")
    Spacer(Modifier.height(3.dp))
    Hint("How the code area itself is drawn.")
    Spacer(Modifier.height(10.dp))
    NumberField(
        label = "Font size (sp)",
        value = e.fontSize,
        min = 8,
        max = 48,
        onChange = { v -> onEditorChange(e.copy(fontSize = v)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Show line numbers",
        description = "Display line numbers in the left gutter.",
        checked = e.showLineNumbers,
        onToggle = { onEditorChange(e.copy(showLineNumbers = !e.showLineNumbers)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Minimap",
        description = "Show minimap on the right (applies after restart).",
        checked = e.showMinimap,
        onToggle = { onEditorChange(e.copy(showMinimap = !e.showMinimap)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Highlight current line",
        description = "Tint the row containing the caret.",
        checked = e.highlightCurrentLine,
        onToggle = { onEditorChange(e.copy(highlightCurrentLine = !e.highlightCurrentLine)) },
    )
    Spacer(Modifier.height(20.dp))
    SectionLabel("Syntax colors")
    Spacer(Modifier.height(3.dp))
    Hint("How far the editor separates calls, members and parameters from plain names.")
    Spacer(Modifier.height(8.dp))
    Row {
        for (preset in SyntaxPreset.entries) {
            ChoiceChip(
                label = preset.label,
                selected = e.syntaxPreset == preset,
                onClick = { onEditorChange(e.copy(syntaxPreset = preset)) },
            )
            Spacer(Modifier.width(6.dp))
        }
    }
    Spacer(Modifier.height(18.dp))
    CheckRow(
        label = "Dim unused symbols",
        description = "Fade locals and imports that nothing else in the file refers to.",
        checked = e.dimUnusedSymbols,
        onToggle = { onEditorChange(e.copy(dimUnusedSymbols = !e.dimUnusedSymbols)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Rainbow brackets",
        description = "Tint each bracket pair and its guide by nesting depth.",
        checked = e.rainbowBrackets,
        onToggle = { onEditorChange(e.copy(rainbowBrackets = !e.rainbowBrackets)) },
    )
    Spacer(Modifier.height(18.dp))
    SectionLabel("Scope guides")
    Spacer(Modifier.height(3.dp))
    Hint("Vertical lines that connect a block to its closing brace.")
    Spacer(Modifier.height(8.dp))
    Row {
        for (mode in ScopeGuideMode.entries) {
            ChoiceChip(
                label = mode.label,
                selected = e.scopeGuides == mode,
                onClick = { onEditorChange(e.copy(scopeGuides = mode)) },
            )
            Spacer(Modifier.width(6.dp))
        }
    }
    Spacer(Modifier.height(18.dp))
    CheckRow(
        label = "Highlight identifier under caret",
        description = "Tint the other places the name at the caret appears.",
        checked = e.highlightIdentifierUnderCaret,
        onToggle = { onEditorChange(e.copy(highlightIdentifierUnderCaret = !e.highlightIdentifierUnderCaret)) },
    )
}
@Composable
private fun RunSection(o: RunOptions, onChange: (RunOptions) -> Unit) {
    CheckRow(
        label = "Clear Output on Run",
        description = "Wipe previous run output before starting.",
        checked = o.clearOutputOnRun,
        onToggle = { onChange(o.copy(clearOutputOnRun = !o.clearOutputOnRun)) },
    )
    Spacer(Modifier.height(8.dp))
    CheckRow(
        label = "Open Terminal on Run",
        description = "Reveal the terminal panel when Run starts.",
        checked = o.openTerminalOnRun,
        onToggle = { onChange(o.copy(openTerminalOnRun = !o.openTerminalOnRun)) },
    )
}

@Composable
private fun KeymapSection() {
    val grouped = remember {
        page.app.input.ActionCatalog.all
            .groupBy { it.context }
            .toSortedMap(compareBy { it.ordinal })
    }
    Hint(
        if (page.ui.Platform.isMac) "Keys shown for macOS."
        else "Keys shown for this machine."
    )
    for ((context, actions) in grouped) {
        Spacer(Modifier.height(10.dp))
        SectionLabel(keymapContextLabel(context))
        Spacer(Modifier.height(2.dp))
        for (action in actions.sortedWith(compareBy({ it.group.ordinal }, { it.label }))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action.label,
                    color = Glass.colors.text,
                    fontSize = Glass.type.label,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = action.group.name,
                    color = Glass.colors.faint,
                    fontSize = Glass.type.label,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = page.app.input.ActionCatalog.label(action) ?: "—",
                    color = Glass.colors.muted,
                    fontSize = Glass.type.label,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}

private fun keymapContextLabel(context: page.app.input.ActionContext): String = when (context) {
    page.app.input.ActionContext.Global -> "Anywhere"
    page.app.input.ActionContext.Editor -> "While editing"
    page.app.input.ActionContext.FileTree -> "In the file tree"
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, color = Glass.colors.text, fontSize = Glass.type.ui, fontWeight = FontWeight.Medium)
}

@Composable
private fun Hint(text: String) {
    Text(text = text, color = Glass.colors.muted, fontSize = Glass.type.label)
}

private fun paletteLabel(p: GlassPalette): String = when (p) {
    GlassPalette.SignatureLight -> "Signature Light"
    else -> p.name
}

@Composable
private fun PaletteSwatch(
    palette: GlassPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    val c = remember(palette) { glassTokensFor(palette).color }
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outline,
        animationSpec = tween(120),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Glass.radius.md))
            .background(colors.surfaceL2)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(Glass.radius.md))
            .clickable(onClick = onClick),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(c.background))
                Box(Modifier.weight(1f).fillMaxHeight().background(c.surface))
                Box(Modifier.weight(1f).fillMaxHeight().background(c.primary))
                Box(Modifier.weight(1f).fillMaxHeight().background(c.accent))
            }
            Text(
                text = paletteLabel(palette),
                color = colors.text,
                fontSize = Glass.type.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 8.dp),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> colors.primarySoft
            hovered -> colors.surfaceL3
            else -> colors.surfaceL2
        },
        animationSpec = tween(120),
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outline,
        animationSpec = tween(120),
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.text,
        animationSpec = tween(120),
    )
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(Glass.radius.sm))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(Glass.radius.sm))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = Glass.type.label,
            style = TextStyle(lineHeight = Glass.type.label, lineHeightStyle = CenteredLineHeight),
        )
    }
}

@Composable
private fun NumberField(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    fun commit() {
        val parsed = text.toIntOrNull()
        val clamped = (parsed ?: value).coerceIn(min, max)
        text = clamped.toString()
        if (clamped != value) onChange(clamped)
    }
    FieldRow(label = label, fieldWidth = 96.dp) {
        BasicTextField(
            value = text,
            onValueChange = { raw -> text = raw.filter { it.isDigit() }.take(4) },
            singleLine = true,
            cursorBrush = SolidColor(Glass.colors.primary),
            textStyle = LocalTextStyle.current.copy(
                fontSize = Glass.type.ui,
                color = Glass.colors.text,
                lineHeight = Glass.type.ui,
                lineHeightStyle = CenteredLineHeight,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.onFocusChanged { if (!it.isFocused) commit() },
        )
    }
}

@Composable
private fun PathField(label: String, value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    fun commit() {
        val trimmed = text.trim()
        text = trimmed
        if (trimmed != value) onChange(trimmed)
    }
    FieldRow(label = label, fieldWidth = null) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            cursorBrush = SolidColor(Glass.colors.primary),
            textStyle = LocalTextStyle.current.copy(
                fontSize = Glass.type.ui,
                color = Glass.colors.text,
                lineHeight = Glass.type.ui,
                lineHeightStyle = CenteredLineHeight,
            ),
            modifier = Modifier.onFocusChanged { if (!it.isFocused) commit() },
        )
    }
}

@Composable
private fun FieldRow(label: String, fieldWidth: androidx.compose.ui.unit.Dp?, field: @Composable () -> Unit) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val hoverBg = colors.surfaceRaised
    val rowBg by animateColorAsState(
        targetValue = if (hovered) hoverBg else hoverBg.copy(alpha = 0f),
        animationSpec = tween(120),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Glass.radius.sm))
            .background(rowBg)
            .hoverable(interaction)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.muted,
            fontSize = Glass.type.label,
            style = TextStyle(lineHeight = Glass.type.label, lineHeightStyle = CenteredLineHeight),
            modifier = Modifier.width(150.dp),
        )
        Box(
            modifier = Modifier
                .let { if (fieldWidth != null) it.width(fieldWidth) else it.weight(1f) }
                .height(30.dp)
                .clip(RoundedCornerShape(Glass.radius.xs))
                .background(colors.surfaceL2)
                .border(1.dp, colors.outline, RoundedCornerShape(Glass.radius.xs))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            field()
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val hoverBg = colors.surfaceRaised
    val rowBg by animateColorAsState(
        targetValue = if (hovered) hoverBg else hoverBg.copy(alpha = 0f),
        animationSpec = tween(120),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Glass.radius.sm))
            .background(rowBg)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = colors.text,
                fontSize = Glass.type.ui,
                style = TextStyle(lineHeight = Glass.type.ui, lineHeightStyle = CenteredLineHeight),
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = description,
                color = colors.muted,
                fontSize = Glass.type.label,
                style = TextStyle(lineHeight = Glass.type.label, lineHeightStyle = CenteredLineHeight),
            )
        }
        Spacer(Modifier.width(12.dp))
        ToggleSwitch(checked = checked)
    }
}

@Composable
private fun ToggleSwitch(checked: Boolean) {
    val colors = Glass.colors
    val trackBg by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.surfaceL2,
        animationSpec = tween(150),
    )
    val trackBorder by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.outline,
        animationSpec = tween(150),
    )
    val knobX by animateDpAsState(if (checked) 16.dp else 0.dp, tween(160))
    val knobColor by animateColorAsState(
        targetValue = if (checked) colors.onPrimary else colors.muted,
        animationSpec = tween(150),
    )
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(trackBg)
            .border(1.dp, trackBorder, RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
                .offset(x = knobX)
                .size(16.dp)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}

@Composable
private fun GlassButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            primary && hovered -> colors.primary.copy(alpha = colors.primary.alpha * 0.85f)
            primary -> colors.primary
            hovered -> colors.surfaceL3
            else -> colors.surfaceL2
        },
        animationSpec = tween(120),
    )
    Box(
        modifier = Modifier
            .height(30.dp)
            .defaultMinSize(minWidth = 84.dp)
            .clip(RoundedCornerShape(Glass.radius.sm))
            .background(bg)
            .border(1.dp, if (primary) Color.Transparent else colors.outline, RoundedCornerShape(Glass.radius.sm))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (primary) colors.onPrimary else colors.text,
            fontSize = Glass.type.ui,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
            style = TextStyle(lineHeight = Glass.type.ui, lineHeightStyle = CenteredLineHeight),
        )
    }
}
