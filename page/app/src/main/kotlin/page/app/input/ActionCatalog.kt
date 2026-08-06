package page.app.input

import androidx.compose.ui.input.key.Key

enum class ActionGroup { File, Edit, Navigate, Code, View, Page }

enum class SearchMode { Any, OnlyWhenOpen, OnlyWhenClosed }

data class Binding(
    val key: Key,
    val primary: Boolean = false,
    val control: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
) {
    fun matches(key: Key, primary: Boolean, control: Boolean, alt: Boolean, shift: Boolean): Boolean =
        this.key == key &&
            this.primary == primary &&
            this.control == control &&
            this.alt == alt &&
            this.shift == shift
}

data class ActionSpec(
    val id: String,
    val label: String,
    val group: ActionGroup,
    val binding: Binding?,
    val macBinding: Binding? = binding,
    val searchMode: SearchMode = SearchMode.Any,
    val run: (ActionHost) -> Unit,
) {
    fun bindingFor(mac: Boolean): Binding? = if (mac) macBinding else binding
}

object ActionCatalog {

    val all: List<ActionSpec> = listOf(
        ActionSpec(
            id = "file.openFile",
            label = "Open File…",
            group = ActionGroup.File,
            binding = Binding(Key.O, primary = true),
        ) { it.openFile() },
        ActionSpec(
            id = "file.openFolder",
            label = "Open Folder…",
            group = ActionGroup.File,
            binding = Binding(Key.O, primary = true, shift = true),
        ) { it.openFolder() },
        ActionSpec(
            id = "file.save",
            label = "Save",
            group = ActionGroup.File,
            binding = Binding(Key.S, primary = true),
        ) { it.saveFile() },
        ActionSpec(
            id = "file.closeTab",
            label = "Close Tab",
            group = ActionGroup.File,
            binding = Binding(Key.W, primary = true),
        ) { it.closeActiveTab() },
        ActionSpec(
            id = "file.settings",
            label = "Settings…",
            group = ActionGroup.File,
            binding = Binding(Key.S, primary = true, alt = true),
            macBinding = Binding(Key.Comma, primary = true),
        ) { it.openSettings() },
        ActionSpec(
            id = "file.refreshTree",
            label = "Refresh File Tree",
            group = ActionGroup.File,
            binding = Binding(Key.F5),
        ) { it.refreshTree() },

        ActionSpec(
            id = "edit.undo",
            label = "Undo",
            group = ActionGroup.Edit,
            binding = Binding(Key.Z, primary = true),
            searchMode = SearchMode.OnlyWhenClosed,
        ) { it.requestUndo() },
        ActionSpec(
            id = "edit.redo",
            label = "Redo",
            group = ActionGroup.Edit,
            binding = Binding(Key.Z, primary = true, shift = true),
            searchMode = SearchMode.OnlyWhenClosed,
        ) { it.requestRedo() },
        ActionSpec(
            id = "edit.redoAlternate",
            label = "Redo",
            group = ActionGroup.Edit,
            binding = Binding(Key.Y, primary = true),
            searchMode = SearchMode.OnlyWhenClosed,
        ) { it.requestRedo() },
        ActionSpec(
            id = "edit.find",
            label = "Find",
            group = ActionGroup.Edit,
            binding = Binding(Key.F, primary = true),
        ) { it.openSearch() },
        ActionSpec(
            id = "edit.replace",
            label = "Replace",
            group = ActionGroup.Edit,
            binding = Binding(Key.R, primary = true),
        ) { it.openReplace() },
        ActionSpec(
            id = "edit.closeSearch",
            label = "Close Search",
            group = ActionGroup.Edit,
            binding = Binding(Key.Escape),
            searchMode = SearchMode.OnlyWhenOpen,
        ) { it.closeSearch() },

        ActionSpec(
            id = "nav.quickOpen",
            label = "Go to File…",
            group = ActionGroup.Navigate,
            binding = Binding(Key.P, primary = true),
        ) { it.openQuickOpen() },
        ActionSpec(
            id = "nav.workspaceSymbol",
            label = "Go to Symbol in Project…",
            group = ActionGroup.Navigate,
            binding = Binding(Key.T, primary = true),
        ) { it.openWorkspaceSymbol() },
        ActionSpec(
            id = "nav.documentSymbol",
            label = "Go to Symbol in File…",
            group = ActionGroup.Navigate,
            binding = Binding(Key.F12, primary = true),
        ) { it.openDocumentSymbol() },
        ActionSpec(
            id = "nav.findInFiles",
            label = "Find in Files",
            group = ActionGroup.Navigate,
            binding = Binding(Key.F, primary = true, shift = true),
        ) { it.toggleFindInFiles() },
        ActionSpec(
            id = "nav.nextProblem",
            label = "Next Problem",
            group = ActionGroup.Navigate,
            binding = Binding(Key.F8),
        ) { it.jumpProblemRelative(true) },
        ActionSpec(
            id = "nav.prevProblem",
            label = "Previous Problem",
            group = ActionGroup.Navigate,
            binding = Binding(Key.F8, shift = true),
        ) { it.jumpProblemRelative(false) },
        ActionSpec(
            id = "nav.prevTab",
            label = "Previous Tab",
            group = ActionGroup.Navigate,
            binding = Binding(Key.DirectionLeft, alt = true),
            searchMode = SearchMode.OnlyWhenClosed,
        ) { it.activateAdjacentTab(-1) },
        ActionSpec(
            id = "nav.nextTab",
            label = "Next Tab",
            group = ActionGroup.Navigate,
            binding = Binding(Key.DirectionRight, alt = true),
            searchMode = SearchMode.OnlyWhenClosed,
        ) { it.activateAdjacentTab(1) },

        ActionSpec(
            id = "code.format",
            label = "Format File",
            group = ActionGroup.Code,
            binding = Binding(Key.Enter, alt = true, shift = true),
        ) { it.triggerFormat() },
        ActionSpec(
            id = "code.action",
            label = "Show Context Actions",
            group = ActionGroup.Code,
            binding = Binding(Key.Enter, alt = true),
            searchMode = SearchMode.OnlyWhenClosed,
        ) { it.triggerCodeAction() },

        ActionSpec(
            id = "view.problems",
            label = "Problems",
            group = ActionGroup.View,
            binding = Binding(Key.M, primary = true, shift = true),
        ) { it.toggleProblems() },
        ActionSpec(
            id = "view.todo",
            label = "Todo",
            group = ActionGroup.View,
            binding = Binding(Key.Six, primary = true, shift = true),
        ) { it.toggleTodo() },
        ActionSpec(
            id = "view.split",
            label = "Split Editor",
            group = ActionGroup.View,
            binding = Binding(Key.Backslash, primary = true),
        ) { it.toggleSplit() },
        ActionSpec(
            id = "view.splitFlip",
            label = "Flip Split Orientation",
            group = ActionGroup.View,
            binding = Binding(Key.Backslash, primary = true, shift = true),
        ) { it.toggleSplitOrientation() },

        ActionSpec(
            id = "page.cycleTheme",
            label = "Next Theme",
            group = ActionGroup.Page,
            binding = Binding(Key.T, primary = true, alt = true),
            macBinding = Binding(Key.T, control = true, alt = true),
        ) { it.cyclePalette() },
        ActionSpec(
            id = "page.atlasFocus",
            label = "Focus in Atlas",
            group = ActionGroup.Page,
            binding = Binding(Key.A, primary = true, alt = true),
            macBinding = Binding(Key.A, control = true, alt = true),
        ) { it.focusActiveInAtlas() },
    )

    fun resolve(
        key: Key,
        primary: Boolean,
        control: Boolean,
        alt: Boolean,
        shift: Boolean,
        hasSearch: Boolean,
        mac: Boolean = Platform.isMac,
    ): ActionSpec? = all.firstOrNull { spec ->
        val binding = spec.bindingFor(mac) ?: return@firstOrNull false
        if (!binding.matches(key, primary, control, alt, shift)) return@firstOrNull false
        when (spec.searchMode) {
            SearchMode.Any -> true
            SearchMode.OnlyWhenOpen -> hasSearch
            SearchMode.OnlyWhenClosed -> !hasSearch
        }
    }

    fun label(spec: ActionSpec, mac: Boolean = Platform.isMac): String? =
        spec.bindingFor(mac)?.let { ShortcutLabels.of(it, mac) }

    fun labelOf(id: String, mac: Boolean = Platform.isMac): String? =
        all.firstOrNull { it.id == id }?.let { label(it, mac) }
}
