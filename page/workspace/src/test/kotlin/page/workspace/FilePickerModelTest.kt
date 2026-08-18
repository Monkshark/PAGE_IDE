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
    fun `a dot name counts as hidden on every platform`() {
        assertTrue(FilePickerModel.isHidden(".cargo", null))
        assertTrue(FilePickerModel.isHidden(".build", false))
        assertFalse(FilePickerModel.isHidden("build", null))
        assertTrue(FilePickerModel.isHidden("AppData", true), "windows marks some plain names hidden")
    }

    @Test
    fun `hiding config folders leaves the ordinary ones`() {
        val entries = listOf(
            PickerEntry(Path.of(".cargo"), ".cargo", true, 0L, isHidden = true),
            PickerEntry(Path.of("Desktop"), "Desktop", true, 0L),
            PickerEntry(Path.of(".gradle"), ".gradle", true, 0L, isHidden = true),
            PickerEntry(Path.of("go"), "go", true, 0L),
        )
        assertEquals(
            listOf("Desktop", "go"),
            FilePickerModel.visible(entries, "", PickMode.OPEN_FOLDER, showHidden = false).map { it.name },
        )
        assertEquals(4, FilePickerModel.visible(entries, "", PickMode.OPEN_FOLDER, showHidden = true).size)
        assertEquals(2, FilePickerModel.hiddenCount(entries))
    }

    @Test
    fun `the filter still applies while hidden entries are away`() {
        val entries = listOf(
            PickerEntry(Path.of(".gradle"), ".gradle", true, 0L, isHidden = true),
            PickerEntry(Path.of("gradle"), "gradle", true, 0L),
            PickerEntry(Path.of("go"), "go", true, 0L),
        )
        assertEquals(
            listOf("gradle"),
            FilePickerModel.visible(entries, "grad", PickMode.OPEN_FOLDER, showHidden = false).map { it.name },
        )
    }

    @Test
    fun `a real listing marks dot folders hidden`() {
        val root = sample()
        Files.createDirectories(root.resolve(".cargo"))
        val listing = FilePickerModel.list(root)
        assertTrue(listing is PickerListing.Ready)
        val hidden = (listing as PickerListing.Ready).entries.first { it.name == ".cargo" }
        assertTrue(hidden.isHidden)
        assertFalse(listing.entries.first { it.name == "src" }.isHidden)
    }

    @Test
    fun `the trail runs from the root down to the folder`() {
        val root = sample()
        val trail = FilePickerModel.trail(root.resolve("src"))
        assertEquals(root.resolve("src").toAbsolutePath().normalize(), trail.last())
        assertEquals(root.toAbsolutePath().normalize(), trail[trail.lastIndex - 1])
        assertNull(trail.first().parent, "the first step should be a filesystem root")
    }

    @Test
    fun `only the deepest levels get a column`() {
        val trail = listOf("/a", "/a/b", "/a/b/c", "/a/b/c/d", "/a/b/c/d/e").map { Path.of(it) }
        val columns = FilePickerModel.columns(trail, max = 3)
        assertEquals(listOf("/a/b/c", "/a/b/c/d", "/a/b/c/d/e").map { Path.of(it) }, columns)
    }

    @Test
    fun `a short trail keeps every level`() {
        val trail = listOf("/a", "/a/b").map { Path.of(it) }
        assertEquals(trail, FilePickerModel.columns(trail, max = 3))
        assertEquals(emptyList(), FilePickerModel.columns(emptyList(), max = 3))
    }

    @Test
    fun `a column knows which of its children the trail went through`() {
        val trail = listOf("/a", "/a/b", "/a/b/c").map { Path.of(it) }
        assertEquals(Path.of("/a/b"), FilePickerModel.childOnTrail(trail, Path.of("/a")))
        assertEquals(Path.of("/a/b/c"), FilePickerModel.childOnTrail(trail, Path.of("/a/b")))
        assertNull(FilePickerModel.childOnTrail(trail, Path.of("/a/b/c")), "the last column leads nowhere")
        assertNull(FilePickerModel.childOnTrail(trail, Path.of("/elsewhere")))
    }

    @Test
    fun `a column is titled by its own name and a root by its letter`() {
        assertEquals("b", FilePickerModel.columnLabel(Path.of("/a/b")))
        val root = FilePickerModel.roots().firstOrNull()
        if (root != null) assertTrue(FilePickerModel.columnLabel(root).isNotBlank())
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
