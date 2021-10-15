package ru.hse.hseditor.presentation.states

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import ru.hse.hseditor.domain.text.PieceTree
import ru.hse.hseditor.domain.text.PieceTreeBuilder
import ru.hse.hseditor.presentation.utils.isRelevant
import kotlin.math.max
import kotlin.math.min

class EditorState(
    var fileName: String,
    var isActive: Boolean = false,
    var content: PieceTree = PieceTreeBuilder().build(),
) {

    var carriagePosition = 0

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.isRelevant()) {
            handleKeyEvent(keyEvent)
        }
        return true
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
        carriagePosition = min(carriagePosition + 1, content.textLength)
    }

    private fun onPressedBackspace() {
        content.deleteAfter(carriagePosition - 1)
        onPressedLeftArrow() // Step to the left
    }

    private fun onTypedChar(char: Char) {
        content.insert(char.toString(), carriagePosition, true)
        carriagePosition++
    }
}
