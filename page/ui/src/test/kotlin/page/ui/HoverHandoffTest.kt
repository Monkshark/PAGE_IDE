package page.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class HoverHandoffTest {

    @Test
    fun `the hover is not dropped the instant the pointer leaves the text`() = runComposeUiTest {
        mainClock.autoAdvance = false
        val hovered = mutableListOf<Int?>()
        setContent {
            GlassTheme {
                Box(Modifier.size(400.dp).testTag("editor")) {
                    CodeEditor(
                        value = TextFieldValue("let value = 1"),
                        onValueChange = {},
                        onHover = { hovered += it },
                        hoverText = "documented thing",
                    )
                }
            }
        }
        repeat(3) { mainClock.advanceTimeByFrame() }

        onNodeWithTag("editor").performMouseInput {
            enter(Offset(40f, 20f))
            moveTo(Offset(41f, 20f))
        }
        repeat(3) { mainClock.advanceTimeByFrame() }
        assertTrue(hovered.isNotEmpty(), "moving over the text should ask for a hover")
        assertNotNull(hovered.last(), "moving over the text should ask for a hover")

        onNodeWithTag("editor").performMouseInput { exit() }
        mainClock.advanceTimeBy(100)
        mainClock.advanceTimeByFrame()
        assertNotNull(hovered.last(), "leaving must leave time to move onto the popup")

        mainClock.advanceTimeBy(400)
        repeat(3) { mainClock.advanceTimeByFrame() }
        assertEquals(null, hovered.last(), "once the handoff window passes the hover closes")
    }
}
