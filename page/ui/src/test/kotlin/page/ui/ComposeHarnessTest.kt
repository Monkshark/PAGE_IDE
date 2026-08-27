package page.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ComposeHarnessTest {

    @Test
    fun `the harness can render and find a composable`() = runComposeUiTest {
        setContent { Text("hello from the harness") }
        onNodeWithText("hello from the harness").assertIsDisplayed()
    }
}
