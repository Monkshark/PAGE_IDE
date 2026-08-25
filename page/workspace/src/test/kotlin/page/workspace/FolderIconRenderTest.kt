package page.workspace

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.svg.SVGDOM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FolderIconRenderTest {

    private val size = 64

    private fun render(icon: String): Bitmap {
        val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream("fileicons/$icon.svg")) {
            "$icon.svg is not on the classpath"
        }.use { it.readBytes() }
        val dom = SVGDOM(Data.makeFromBytes(bytes))
        dom.setContainerSize(size.toFloat(), size.toFloat())
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(size, size)
        bitmap.erase(0)
        dom.render(Canvas(bitmap))
        return bitmap
    }

    private fun Bitmap.at(x: Double, y: Double): String {
        val px = ((x / 16.0) * size).toInt().coerceIn(0, size - 1)
        val py = ((y / 16.0) * size).toInt().coerceIn(0, size - 1)
        val argb = getColor(px, py)
        return String.format("#%06X", argb and 0xFFFFFF)
    }

    private fun Bitmap.colours(): Set<String> {
        val seen = mutableSetOf<String>()
        for (y in 0 until size) for (x in 0 until size) {
            val argb = getColor(x, y)
            if (argb ushr 24 > 250) seen.add(String.format("#%06X", argb and 0xFFFFFF))
        }
        return seen
    }

    private val added = listOf(
        "folder-ruby" to ("#C62828" to "#FFCDD2"),
        "folder-swift" to ("#E64A19" to "#FFCCBC"),
        "folder-c" to ("#0277BD" to "#B3E5FC"),
        "folder-cpp" to ("#01579B" to "#B3E5FC"),
        "folder-bash" to ("#43A047" to "#C8E6C9"),
        "folder-web" to ("#546E7A" to "#CFD8DC"),
    )

    @Test
    fun `every new icon paints its folder and its mark`() {
        for ((icon, palette) in added) {
            val (folder, motive) = palette
            val painted = render(icon).colours()
            assertTrue(folder in painted, "$icon never painted its folder colour $folder, got $painted")
            assertTrue(motive in painted, "$icon never painted its mark colour $motive, got $painted")
        }
    }

    @Test
    fun `a mark never spills outside the folder`() {
        for ((icon, _) in added) {
            val bitmap = render(icon)
            assertEquals("#000000", bitmap.at(0.5, 0.5), "$icon painted above the folder")
            assertEquals("#000000", bitmap.at(8.0, 15.5), "$icon painted below the folder")
        }
    }

    @Test
    fun `the globe is a ring, not a disc`() {
        val bitmap = render("folder-web")
        assertEquals("#546E7A", bitmap.at(11.5, 8.4), "the globe centre should show the folder through it")
        assertEquals("#CFD8DC", bitmap.at(11.5, 6.7), "the globe outline should be drawn")
    }

    @Test
    fun `the c and cpp marks share a baseline so the pair reads as a family`() {
        val c = render("folder-c")
        val cpp = render("folder-cpp")
        fun lowestMark(bitmap: Bitmap, colour: String): Int {
            for (y in size - 1 downTo 0) {
                for (x in 0 until size) {
                    val argb = bitmap.getColor(x, y)
                    if (argb ushr 24 > 250 && String.format("#%06X", argb and 0xFFFFFF) == colour) return y
                }
            }
            return -1
        }
        assertEquals(lowestMark(c, "#B3E5FC"), lowestMark(cpp, "#B3E5FC"))
    }
}
