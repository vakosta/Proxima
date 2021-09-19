package presentation.windows

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import presentation.utils.SplitterState
import presentation.utils.VerticalSplittable
import presentation.utils.codeView
import presentation.views.CodeView

var codeBlockWidth = 300
var codeBlockHeight = 300

var fileContent = ""
var fileContentRendered by mutableStateOf(fileContent.codeView(codeBlockWidth, codeBlockHeight))

fun onKeyEvent(keyEvent: KeyEvent): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.utf16CodePoint < 65000) {
        fileContent = when (keyEvent.utf16CodePoint) {
            8 -> fileContent.dropLast(1)                            // Backspace
            else -> fileContent.plus(keyEvent.utf16CodePoint.toChar()) // Any char
        }
        fileContentRendered = fileContent.codeView(codeBlockWidth, codeBlockHeight)
    }
    return true
}

@Composable
fun MainWindow(state: WindowState) = Window(
    onCloseRequest = {},
    state = state,
    onKeyEvent = ::onKeyEvent
) {

    val panelState = remember { PanelState() }

    VerticalSplittable(
        Modifier.fillMaxSize(),
        panelState.splitter,
        onResize = {
            panelState.expandedSize = (panelState.expandedSize + it).coerceAtLeast(panelState.expandedSizeMin)
        }
    ) {
        Row(modifier = Modifier.width(100.dp)) {
            Text("321")
        }
        CodeView(
            code = fileContentRendered,
            onGloballyPositioned = {
                codeBlockWidth = it.size.width
                codeBlockHeight = it.size.height
                fileContentRendered = fileContent.codeView(codeBlockWidth, codeBlockHeight)
            }
        )
    }
}

private class PanelState {
    val collapsedSize = 24.dp
    var expandedSize by mutableStateOf(300.dp)
    val expandedSizeMin = 90.dp
    var isExpanded by mutableStateOf(true)
    val splitter = SplitterState()
}
