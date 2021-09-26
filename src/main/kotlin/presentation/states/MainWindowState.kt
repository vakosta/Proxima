package presentation.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import domain.skija.SkijaBuilder
import domain.text.PieceTreeBuilder
import presentation.model.File
import presentation.model.getFile
import presentation.utils.isRelevant
import kotlin.math.max
import kotlin.math.min

class MainWindowState(
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
) : WindowState {

    val fileTreeState = FileTree(File("Kek", true, listOf(getFile(), getFile(), getFile()), true))

    private var fileContent = PieceTreeBuilder().build() // Build an empty piece tree, file loading will return a non-empty one
    private var carriagePosition = 0

    val panelState by mutableStateOf(PanelState())
    var fileContentRendered by mutableStateOf(
        SkijaBuilder(
            fileContent.getLinesRawContent(), // Start at EditorRange() and end at EditorRange(), will be faster
            carriagePosition,
            300,
            300
        ).buildView()
    )

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.isRelevant()) {
            handleKeyEvent(keyEvent)
            updateRenderedContent(fileContentRendered.width, fileContentRendered.height)
        }
        return true
    }

    fun updateRenderedContent(width: Int, height: Int) {
        // Start at EditorRange() and end at EditorRange(), will be faster
        fileContentRendered = SkijaBuilder(fileContent.getLinesRawContent(), carriagePosition, width, height).buildView()
    }

    private fun handleKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.nativeKeyEvent.keyCode) {
            8 ->
                onPressedBackspace()
            37 ->
                onPressedLeftArrow()
            39 ->
                onPressedRightArrow()
            else ->
                onTypedChar(keyEvent.utf16CodePoint.toChar())
        }
    }

    private fun onPressedLeftArrow() {
        carriagePosition = max(carriagePosition - 1, 0)
    }

    private fun onPressedRightArrow() {
        carriagePosition = min(carriagePosition + 1, fileContent.textLength)
    }

    private fun onPressedBackspace() {
        fileContent.deleteAfter(carriagePosition - 1)
        onPressedLeftArrow() // Step to the left
    }

    private fun onTypedChar(char: Char) {
        fileContent.insert(char.toString(), carriagePosition, true)
        carriagePosition++
    }
}
