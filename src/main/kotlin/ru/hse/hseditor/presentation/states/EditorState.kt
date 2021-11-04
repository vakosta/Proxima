package ru.hse.hseditor.presentation.states

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.presentation.utils.isRelevant

class EditorState(
    private val myLifetime: Lifetime,
    var fileName: String,
    var isActive: Boolean = false,
    val textState: TextState
) {

    var carriagePosition = 0

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.isRelevant()) {
            handleKeyEvent(keyEvent)
            return true
        }
        return false
    }

    private fun handleKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.nativeKeyEvent.keyCode) {
            8 -> textState.onPressedBackspace()
            37 -> textState.onPressedLeftArrow()
            39 -> textState.onPressedRightArrow()
            else -> textState.onTypedChar(keyEvent.utf16CodePoint.toChar())
        }
    }
}
