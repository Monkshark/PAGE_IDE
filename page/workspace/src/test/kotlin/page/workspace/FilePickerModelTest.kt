package page.workspace

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilePickerModelTest {

    private fun sample(): Path {
        val root = Files.createTempDirectory("picker-")
        Files.createDirectories(root.resolve("src"))
        Files.createDirectories(root.resolve("Build"))
        Files.writeString(root.resolve("Cargo.toml"), "[package]")
        Files.writeString(root.resolve("readme.md"), "hi")
        return root
    }

    private fun entry(name: String, directory: Boolean) =
        PickerEntry(Path.of(name), name, directory, 0L)

    @Test
    fun `folders come first and names sort without regard to case`() {
        val sorted = FilePickerModel.sortEntries(
            listOf(
                entry("readme.md", false),
                entry("src", true),
                entry("Cargo.toml", false),
                entry("Build", true),
            ),
        )
        assertEquals(listOf("Build", "src", "Cargo.toml", "readme.md"), sorted.map { it.name })
    }

    @Test
    fun `listing a real directory reports both kinds`() {
        val root = sample()
        val listing = FilePickerModel.list(root)
        assertTrue(listing is PickerListing.Ready, "expected Ready, got $listing")
        val names = (listing as PickerListing.Ready).entries.map { it.name }
        assertEquals(listOf("Build", "src", "Cargo.toml", "readme.md"), names)
        assertTrue(listing.entries.first { it.name == "Cargo.toml" }.sizeBytes > 0)
    }

    @Test
    fun `a folder that is not there explains itself`() {
        val missing = Files.createTempDirectory("picker-").resolve("gone")
        val listing = FilePickerModel.list(missing)
        assertTrue(listing is PickerListing.Denied, "expected Denied, got $listing")
        assertTrue((listing as PickerListing.Denied).reason.isNotBlank())
    }

    @Test
    fun `a file where a folder was expected says so`() {
        val file = Files.createTempFile("picker-", ".txt")
        val listing = FilePickerModel.list(file)
        assertTrue(listing is PickerListing.Denied)
    }

    @Test
    fun `the filter keeps only matching names`() {
        val entries = listOf(entry("src", true), entry("Cargo.toml", false), entry("readme.md", false))
        val hits = FilePickerModel.visible(entries, "ar", PickMode.OPEN_FILE)
        assertEquals(listOf("Cargo.toml"), hits.map { it.name })
    }

    @Test
    fun `filtering in folder mode never offers a file`() {
        val entries = listOf(entry("target", true), entry("target.txt", false))
        val hits = FilePickerModel.visible(entries, "target", PickMode.OPEN_FOLDER)
        assertEquals(listOf("target"), hits.map { it.name })
    }

    @Test
    fun `an empty filter leaves the listing alone`() {
        val entries = listOf(entry("src", true), entry("a.txt", false))
        assertEquals(entries, FilePickerModel.visible(entries, "   ", PickMode.OPEN_FILE))
    }

    @Test
    fun `crumbs walk from the root down to the folder`() {
        val root = sample()
        val crumbs = FilePickerModel.crumbs(root.resolve("src"))
        assertEquals("src", crumbs.last().label)
        assertEquals(root.resolve("src").toAbsolutePath().normalize(), crumbs.last().path)
        assertTrue(crumbs.size >= 2, "expected a trail, got ${crumbs.map { it.label }}")
    }

    @Test
    fun `a start path that is a file opens its folder`() {
        val root = sample()
        assertEquals(
            root.toAbsolutePath().normalize(),
            FilePickerModel.startingDirectory(root.resolve("Cargo.toml")),
        )
    }

    @Test
    fun `a start path that no longer exists climbs to one that does`() {
        val root = sample()
        assertEquals(
            root.toAbsolutePath().normalize(),
            FilePickerModel.startingDirectory(root.resolve("gone").resolve("deeper")),
        )
    }

    @Test
    fun `opening a file needs a file to be chosen`() {
        val dir = sample()
        assertFalse(FilePickerModel.canConfirm(PickMode.OPEN_FILE, dir, null, ""))
        assertFalse(FilePickerModel.canConfirm(PickMode.OPEN_FILE, dir, entry("src", true), ""))
        assertTrue(FilePickerModel.canConfirm(PickMode.OPEN_FILE, dir, entry("a.txt", false), ""))
    }

    @Test
    fun `opening a folder falls back to the one being browsed`() {
        val dir = sample()
        assertTrue(FilePickerModel.canConfirm(PickMode.OPEN_FOLDER, dir, null, ""))
        assertEquals(dir, FilePickerModel.target(PickMode.OPEN_FOLDER, dir, null, ""))
    }

    @Test
    fun `saving builds the path from the folder and the typed name`() {
        val dir = sample()
        assertEquals(
            dir.resolve("Greeter.kt"),
            FilePickerModel.target(PickMode.SAVE_AS, dir, null, "  Greeter.kt  "),
        )
    }

    @Test
    fun `a name with a path separator is refused`() {
        assertNotNull(FilePickerModel.nameError("src/Greeter.kt"))
        assertNotNull(FilePickerModel.nameError("a:b"))
        assertNotNull(FilePickerModel.nameError("   "))
        assertNotNull(FilePickerModel.nameError(".."))
        assertNull(FilePickerModel.nameError("Greeter.kt"))
    }

    @Test
    fun `saving over an existing file is reported before it happens`() {
        val dir = sample()
        assertNotNull(FilePickerModel.overwriteTarget(PickMode.SAVE_AS, dir, "Cargo.toml"))
        assertNull(FilePickerModel.overwriteTarget(PickMode.SAVE_AS, dir, "new.txt"))
        assertNull(FilePickerModel.overwriteTarget(PickMode.OPEN_FILE, dir, "Cargo.toml"))
    }

    @Test
    fun `sizes read in the unit that fits`() {
        assertEquals("318 B", FilePickerModel.formatSize(318))
        assertEquals("1.2 KB", FilePickerModel.formatSize(1229))
        assertEquals("2.4 MB", FilePickerModel.formatSize(2_517_000))
    }

    @Test
    fun `each mode names its own button`() {
        assertEquals("Open", FilePickerModel.confirmLabel(PickMode.OPEN_FOLDER))
        assertEquals("Save", FilePickerModel.confirmLabel(PickMode.SAVE_AS))
        assertEquals("Create", FilePickerModel.confirmLabel(PickMode.NEW_PROJECT))
        assertTrue(FilePickerModel.needsName(PickMode.NEW_PROJECT))
        assertFalse(FilePickerModel.needsName(PickMode.OPEN_FOLDER))
    }
}
