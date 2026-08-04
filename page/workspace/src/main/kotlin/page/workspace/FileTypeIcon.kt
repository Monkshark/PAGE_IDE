package page.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.ui.Glass
import java.nio.file.Path

private val CenterTight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

@Composable
fun FileTypeIcon(path: Path, isDirectory: Boolean, size: Dp = 15.dp) {
    val res = FileIcons.resourceFor(path, isDirectory)
    val ext = if (isDirectory) "" else path.fileName?.toString()?.substringAfterLast('.', "").orEmpty()
    when {
        res != null -> Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
        ext.isNotEmpty() -> FileExtChip(ext.take(4).lowercase(), size)
        else -> Image(
            painter = painterResource(FileIcons.documentResource()),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun FileExtChip(ext: String, size: Dp) {
    val colors = Glass.colors
    Box(
        modifier = Modifier
            .size(width = size + 5.dp, height = size)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.surfaceL3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ext,
            color = colors.muted,
            style = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                lineHeight = 7.sp,
                lineHeightStyle = CenterTight,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
