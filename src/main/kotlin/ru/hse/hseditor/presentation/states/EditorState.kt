package ru.hse.hseditor.presentation.states

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import org.koin.core.component.KoinComponent
import ru.hse.hseditor.data.CharCoordinates
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.presentation.utils.isRelevant
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import kotlin.math.max
import kotlin.math.min

class EditorState(
    private val myLifetime: Lifetime,
    var fileName: String,
    var isActive: Boolean = false,
    val textState: TextState
) {

    var charCoordinates: MutableList<CharCoordinates> = mutableListOf()

    var verticalOffset: Float = 0F
        private set
    var horizontalOffset: Float = 0F
        private set

    var maxTextX = 0F
    var maxTextY = 0F

    @OptIn(ExperimentalComposeUiApi::class)
    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.isMetaPressed && keyEvent.key == Key.V) {
            handlePasteFromClipboard()
            return true
        } else if (keyEvent.isRelevant()) {
            handleKeyEvent(keyEvent)
            return true
        }
        return false
    }

    fun setVerticalOffset(delta: Float, windowHeight: Int) {
        val scrollMax = max(0F, (maxTextY + 20F) - windowHeight)
        verticalOffset = min(scrollMax, max(0F, verticalOffset + delta))
    }

    fun updateCaretPosition(x: Float, y: Float) {
        val absolutePosition = getCharAbsolutePosition(x, y) ?: return
        if (textState.firstSelectionPosition == null) {
            textState.firstSelectionPosition = absolutePosition
        } else {
            textState.secondSelectionPosition = absolutePosition
        }
        textState.setCaretAbsoluteOffset(absolutePosition, true)
    }

    private fun getCharAbsolutePosition(x: Float, y: Float): Int? =
        charCoordinates
            .binarySearch(CharCoordinates(x, y))
            .let { if (-it - 2 >= 0) charCoordinates[-it - 2] else null }
            ?.absoluteOffset

    private fun handleKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.nativeKeyEvent.keyCode) {
            8 ->
                textState.onPressedBackspace()
            37 ->
                textState.onPressedLeftArrow(false)
            38 ->
                textState.onPressedUpArrow(false)
            39 ->
                textState.onPressedRightArrow(false)
            40 ->
                textState.onPressedDownArrow(false)
            else ->
                textState.onTypedChar(keyEvent.utf16CodePoint.toChar())
        }
    }

    private fun handlePasteFromClipboard() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        textState.onAddText(clipboard)
    }
}
