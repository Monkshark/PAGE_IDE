package page.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import page.ui.Glass
import java.nio.file.Path

data class PickRequest(
    val mode: PickMode,
    val startIn: Path? = null,
    val suggestedName: String = "",
    val onPick: (Path) -> Unit,
)

@Composable
fun FilePickerDialog(request: PickRequest, onDismiss: () -> Unit) {
    val colors = Glass.colors
    val mode = request.mode

    var current by remember(request) { mutableStateOf(FilePickerModel.startingDirectory(request.startIn)) }
    var listing by remember(request) { mutableStateOf<PickerListing?>(null) }
    var selected by remember(request) { mutableStateOf<PickerEntry?>(null) }
    var query by remember(request) { mutableStateOf("") }
    var name by remember(request) { mutableStateOf(request.suggestedName) }
    var overwriting by remember(request) { mutableStateOf<Path?>(null) }

    LaunchedEffect(current) {
        listing = null
        selected = null
        query = ""
        listing = withContext(Dispatchers.IO) { FilePickerModel.list(current) }
    }

    val entries = (listing as? PickerListing.Ready)?.entries.orEmpty()
    val shown = remember(entries, query, mode) { FilePickerModel.visible(entries, query, mode) }
    val nameProblem = if (FilePickerModel.needsName(mode)) FilePickerModel.nameError(name) else null
    val confirmable = FilePickerModel.canConfirm(mode, current, selected, name) && listing is PickerListing.Ready

    fun confirm() {
        if (!confirmable) return
        val target = FilePickerModel.target(mode, current, selected, name) ?: return
        val clash = FilePickerModel.overwriteTarget(mode, current, name)
        if (clash != null && overwriting == null) {
            overwriting = clash
            return
        }
        onDismiss()
        request.onPick(target)
    }

    fun enter(entry: PickerEntry) {
        if (entry.isDirectory) current = entry.path else confirm()
    }

    fun goUp() {
        FilePickerModel.parentOf(current)?.let { current = it }
    }

    val focus = remember(request) { FocusRequester() }
    LaunchedEffect(request) { runCatching { focus.requestFocus() } }

    val shape = RoundedCornerShape(Glass.radius.md)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .height(540.dp)
                .clip(shape)
                .background(colors.surfaceL2)
                .border(1.dp, colors.outline, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Escape -> {
                            if (overwriting != null) overwriting = null else onDismiss()
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            val pick = selected
                            if (pick != null && pick.isDirectory && !FilePickerModel.needsName(mode)) enter(pick)
                            else confirm()
                            true
                        }
                        Key.Backspace -> {
                            if (!FilePickerModel.needsName(mode)) { goUp(); true } else false
                        }
                        Key.DirectionDown -> {
                            val idx = shown.indexOf(selected)
                            selected = shown.getOrNull((idx + 1).coerceAtMost(shown.lastIndex))
                            true
                        }
                        Key.DirectionUp -> {
                            val idx = shown.indexOf(selected)
                            selected = if (idx <= 0) shown.firstOrNull() else shown[idx - 1]
                            true
                        }
                        else -> false
                    }
                },
        ) {
            PickerHeader(mode)
            PickerCrumbs(current, query, { current = it }, { query = it }, ::goUp)
            if (FilePickerModel.needsName(mode)) {
                PickerNameField(name, nameProblem) { name = it }
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                PickerRail(current) { current = it }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(colors.separator))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val state = listing) {
                        null -> PickerMessage("Reading $current…")
                        is PickerListing.Denied -> PickerMessage("This folder could not be opened", state.reason)
                        is PickerListing.Ready -> if (shown.isEmpty()) {
                            PickerMessage(if (query.isBlank()) "Nothing in this folder" else "No match for “$query”")
                        } else {
                            PickerList(shown, selected, mode, { selected = it }, ::enter)
                        }
                    }
                }
            }
            PickerFooter(
                mode = mode,
                path = FilePickerModel.target(mode, current, selected, name)?.toString() ?: current.toString(),
                confirmable = confirmable,
                overwriting = overwriting,
                onCancel = onDismiss,
                onConfirm = ::confirm,
                onKeepEditing = { overwriting = null },
            )
        }
    }
}

@Composable
private fun PickerHeader(mode: PickMode) {
    val colors = Glass.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = FilePickerModel.title(mode),
            style = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Glass.radius.xs))
                .background(colors.primarySoft)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                text = FilePickerModel.modeTag(mode),
                style = LocalTextStyle.current.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                color = colors.primary,
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Glass.colors.separator))
}

@Composable
private fun PickerCrumbs(
    current: Path,
    query: String,
    onNavigate: (Path) -> Unit,
    onQuery: (String) -> Unit,
    onUp: () -> Unit,
) {
    val colors = Glass.colors
    val crumbs = remember(current) { FilePickerModel.crumbs(current) }
    Row(
        modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "↑",
            style = LocalTextStyle.current.copy(fontSize = 12.sp),
            color = colors.muted,
            modifier = Modifier.clip(RoundedCornerShape(Glass.radius.xs)).clickable { onUp() }.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            crumbs.forEachIndexed { index, crumb ->
                if (index > 0) {
                    Text(text = "›", style = LocalTextStyle.current.copy(fontSize = 11.sp), color = colors.faint)
                }
                val here = index == crumbs.lastIndex
                Text(
                    text = crumb.label,
                    style = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    color = if (here) colors.text else colors.muted,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Glass.radius.xs))
                        .background(if (here) colors.surfaceL3 else Color.Transparent)
                        .clickable { onNavigate(crumb.path) }
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        PickerField(
            value = query,
            placeholder = "Filter by name",
            onValue = onQuery,
            width = 180.dp,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Glass.colors.separator))
}

@Composable
private fun PickerNameField(name: String, problem: String?, onName: (String) -> Unit) {
    val colors = Glass.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Name",
            style = LocalTextStyle.current.copy(fontSize = 11.sp),
            color = colors.muted,
            modifier = Modifier.width(42.dp),
        )
        PickerField(value = name, placeholder = "Untitled", onValue = onName, width = 260.dp, focused = true)
        if (problem != null) {
            Text(
                text = problem,
                style = LocalTextStyle.current.copy(fontSize = 11.sp),
                color = colors.danger,
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Glass.colors.separator))
}

@Composable
private fun PickerField(
    value: String,
    placeholder: String,
    onValue: (String) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    focused: Boolean = false,
) {
    val colors = Glass.colors
    val shape = RoundedCornerShape(Glass.radius.xs)
    Box(
        modifier = Modifier
            .width(width)
            .height(24.dp)
            .clip(shape)
            .background(colors.surfaceL1)
            .border(1.dp, if (focused) colors.primary else colors.outline, shape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = LocalTextStyle.current.copy(fontSize = 11.sp),
                color = colors.faint,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(fontSize = 11.sp, color = colors.text, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PickerRail(current: Path, onNavigate: (Path) -> Unit) {
    val colors = Glass.colors
    val roots = remember { FilePickerModel.roots() }
    val home = remember { FilePickerModel.homeDirectory() }
    Column(
        modifier = Modifier.width(168.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 10.dp),
    ) {
        RailLabel("Pinned")
        RailItem("Home", home, current, onNavigate)
        RailItem("Desktop", home.resolve("Desktop"), current, onNavigate)
        RailItem("Documents", home.resolve("Documents"), current, onNavigate)
        Spacer(Modifier.height(12.dp))
        RailLabel("Drives")
        roots.forEach { root ->
            RailItem(root.toString(), root, current, onNavigate)
        }
    }
}

@Composable
private fun RailLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = LocalTextStyle.current.copy(fontSize = 10.sp, letterSpacing = 1.sp),
        color = Glass.colors.faint,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 5.dp),
    )
}

@Composable
private fun RailItem(label: String, path: Path, current: Path, onNavigate: (Path) -> Unit) {
    val colors = Glass.colors
    val on = runCatching { path.toAbsolutePath().normalize() == current }.getOrDefault(false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(if (on) colors.primarySoft else Color.Transparent)
            .clickable { onNavigate(path) }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FileTypeIcon(path = path, isDirectory = true, size = 16.dp)
        Text(
            text = label,
            style = LocalTextStyle.current.copy(fontSize = 12.sp),
            color = if (on) colors.text else colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickerList(
    entries: List<PickerEntry>,
    selected: PickerEntry?,
    mode: PickMode,
    onSelect: (PickerEntry) -> Unit,
    onEnter: (PickerEntry) -> Unit,
) {
    val colors = Glass.colors
    val state = rememberLazyListState()
    LaunchedEffect(selected) {
        val index = entries.indexOf(selected)
        if (index >= 0) runCatching { state.animateScrollToItem(index) }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        itemsIndexed(entries, key = { _, e -> e.path.toString() }) { _, entry ->
            val dimmed = FilePickerModel.picksDirectories(mode) && !entry.isDirectory
            val isSelected = entry == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(if (isSelected) colors.primarySoft else Color.Transparent)
                    .then(if (dimmed) Modifier else Modifier.clickable { onSelect(entry); if (entry.isDirectory) onEnter(entry) })
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    FileTypeIcon(path = entry.path, isDirectory = entry.isDirectory, size = 16.dp)
                }
                Text(
                    text = entry.name,
                    style = LocalTextStyle.current.copy(fontSize = 12.sp),
                    color = if (dimmed) colors.faint else colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (entry.isDirectory) "" else FilePickerModel.formatSize(entry.sizeBytes),
                    style = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    color = colors.faint,
                )
                if (entry.isDirectory) {
                    Text(text = "›", style = LocalTextStyle.current.copy(fontSize = 11.sp), color = colors.faint)
                }
            }
        }
    }
}

@Composable
private fun PickerMessage(title: String, detail: String? = null) {
    val colors = Glass.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = LocalTextStyle.current.copy(fontSize = 12.sp),
            color = colors.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (detail != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = detail,
                style = LocalTextStyle.current.copy(fontSize = 11.sp),
                color = colors.faint,
            )
        }
    }
}

@Composable
private fun PickerFooter(
    mode: PickMode,
    path: String,
    confirmable: Boolean,
    overwriting: Path?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val colors = Glass.colors
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).background(colors.surfaceL1).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (overwriting != null) "ALREADY EXISTS" else "SELECTED",
                style = LocalTextStyle.current.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                color = if (overwriting != null) colors.warn else colors.faint,
            )
            Text(
                text = overwriting?.toString() ?: path,
                style = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (overwriting != null) {
            PickerButton("Keep editing", primary = false, enabled = true, onClick = onKeepEditing)
            PickerButton("Replace", primary = true, enabled = true, onClick = onConfirm)
        } else {
            PickerButton("Cancel", primary = false, enabled = true, onClick = onCancel)
            PickerButton(FilePickerModel.confirmLabel(mode), primary = true, enabled = confirmable, onClick = onConfirm)
        }
    }
}

@Composable
private fun PickerButton(label: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = Glass.colors
    val shape = RoundedCornerShape(Glass.radius.sm)
    val background = when {
        !enabled -> colors.surfaceL3
        primary -> colors.primary
        else -> colors.surfaceL3
    }
    val foreground = when {
        !enabled -> colors.faint
        primary -> colors.onPrimary
        else -> colors.text
    }
    Box(
        modifier = Modifier
            .width(124.dp)
            .height(30.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, if (primary && enabled) colors.primary else colors.outline, shape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LocalTextStyle.current.copy(
                fontSize = 12.sp,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = foreground,
        )
    }
}
