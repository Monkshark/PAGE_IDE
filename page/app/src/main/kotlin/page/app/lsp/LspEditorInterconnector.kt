package page.app.lsp

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import page.app.EditorPaneState
import page.language.LspController
import page.app.PaneSide
import page.app.ReferencesQueryState
import page.app.ReferencesSurface
import page.app.utils.lineCharToOffset
import page.editor.EditHistory
import page.editor.EditSnapshot
import page.editor.FileDocument
import page.editor.OpenTab
import page.lsp.ReferenceLocation
import page.lsp.RenameApply
import page.lsp.RenameWorkspaceEdit
import page.lsp.pickSingleOtherReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.file.Path

internal class LspEditorInterconnector(
    private val scope: CoroutineScope,
    private val focused: () -> EditorPaneState,
    private val paneOf: (PaneSide) -> EditorPaneState,
    private val setPane: (PaneSide, EditorPaneState) -> Unit,
    private val mutatePane: (PaneSide, (EditorPaneState) -> EditorPaneState) -> Unit,
    private val openInTabAt: (Path, Int) -> Unit,
    private val controllerFor: (Path) -> LspController?,
    private val applyExternalChange: (String, String) -> Unit,
    private val getReferences: () -> ReferencesQueryState?,
    private val setReferences: (ReferencesQueryState?) -> Unit,
    private val localUsages: (symbol: String, originUri: String) -> List<ReferenceLocation> = { _, _ -> emptyList() },
    private val isIndexing: () -> Boolean = { false },
) {
    fun jumpToProblem(picked: Path, line: Int, character: Int) {
        val pane = focused()
        val text = pane.book.tabs.firstOrNull { it.path == picked }?.text
            ?: FileDocument.loadOrNull(picked) ?: return
        openInTabAt(picked, lineCharToOffset(text, line, character))
    }

    fun requestReferences(
        p: Path,
        line: Int,
        char: Int,
        symbol: String,
        surface: ReferencesSurface = ReferencesSurface.Panel,
    ) {
        val origin = p.toUri().toString()
        setReferences(
            ReferencesQueryState(
                symbolName = symbol,
                originUri = origin,
                results = emptyList(),
                isLoading = true,
                surface = surface,
            )
        )
        val ctrl = controllerFor(p)
        if (ctrl == null) {
            publishLocalUsages(symbol, origin, surface)
            return
        }
        scope.launch {
            val results = runCatching {
                ctrl.references(p, line, char, includeDeclaration = true, symbolName = symbol).await()
            }.getOrNull()
            run {
                if (results == null) {
                    publishLocalUsages(symbol, origin, surface)
                    return@launch
                }
                val list = results
                if (list.isEmpty()) {
                    publishLocalUsages(symbol, origin, surface)
                    return@launch
                }
                val autoJump = pickSingleOtherReference(list, origin, line, char)
                if (autoJump != null) {
                    setReferences(null)
                    val target = runCatching {
                        java.nio.file.Paths.get(java.net.URI(autoJump.uri))
                    }.getOrNull()
                    if (target != null) {
                        jumpToProblem(target, autoJump.startLine, autoJump.startCharacter)
                    } else {
                        setReferences(
                            ReferencesQueryState(
                                symbolName = symbol,
                                originUri = origin,
                                results = list,
                                isLoading = false,
                                surface = surface,
                            )
                        )
                    }
                } else {
                    setReferences(
                        ReferencesQueryState(
                            symbolName = symbol,
                            originUri = origin,
                            results = list,
                            isLoading = false,
                            surface = surface,
                        )
                    )
                }
            }
        }
    }

    private fun publishLocalUsages(symbol: String, origin: String, surface: ReferencesSurface) {
        setReferences(
            ReferencesQueryState(
                symbolName = symbol,
                originUri = origin,
                results = localUsages(symbol, origin),
                isLoading = false,
                surface = surface,
                indexing = isIndexing(),
            )
        )
    }

    fun applyRename(edit: RenameWorkspaceEdit) {
        val groupId = System.nanoTime()
        for (change in edit.changes) {
            val path = runCatching {
                java.nio.file.Paths.get(java.net.URI(change.uri))
            }.getOrNull() ?: continue
            var handled = false
            for (side in listOf(PaneSide.PRIMARY, PaneSide.SECONDARY)) {
                val p = paneOf(side)
                val idx = p.book.tabs.indexOfFirst { it.path == path }
                if (idx < 0) continue
                handled = true
                val tab = p.book.tabs[idx]
                val newText = RenameApply.applyToText(tab.text, change.edits)
                if (newText == tab.text) continue
                val isActive = idx == p.book.activeIndex
                val priorCaret = if (isActive) p.editorValue.selection.start else tab.caret
                val priorSnapshot = EditSnapshot(tab.text, priorCaret, groupId)
                val newCaretInTab = priorCaret.coerceAtMost(newText.length)
                val updatedTabs = p.book.tabs.toMutableList().also {
                    it[idx] = tab.copy(
                        text = newText,
                        caret = newCaretInTab,
                        history = tab.history.pushBeforeChange(priorSnapshot),
                    )
                }
                val newEditorValue = if (isActive) {
                    TextFieldValue(newText, TextRange(newCaretInTab))
                } else p.editorValue
                setPane(
                    side,
                    p.copy(
                        book = p.book.copy(tabs = updatedTabs),
                        editorValue = newEditorValue,
                    ),
                )
                applyExternalChange(change.uri, newText)
            }
            if (!handled) {
                val onDisk = FileDocument.loadOrNull(path) ?: continue
                val newText = RenameApply.applyToText(onDisk, change.edits)
                if (newText == onDisk) continue
                val priorSnapshot = EditSnapshot(onDisk, 0, groupId)
                val newTab = OpenTab(
                    path = path,
                    text = newText,
                    savedText = onDisk,
                    caret = 0,
                    history = EditHistory().pushBeforeChange(priorSnapshot),
                )
                mutatePane(PaneSide.PRIMARY) {
                    val savedActive = it.book.activeIndex
                    val appended = it.book.appendTab(newTab)
                    val restored = if (savedActive in appended.tabs.indices) appended.copy(activeIndex = savedActive) else appended
                    it.copy(book = restored)
                }
                applyExternalChange(change.uri, newText)
            }
        }
    }
}
