package ru.hse.hseditor.presentation.states

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.presentation.utils.isRelevant

class EditorState(
    var fileName: String,
    var isActive: Boolean = false,
) : KoinComponent {

    val textState: TextState by inject { parametersOf("", TextState.Language.Kotlin) }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.isRelevant()) {
            handleKeyEvent(keyEvent)
        }
        return true
    }

    private fun handleKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.nativeKeyEvent.keyCode) {
            8 ->
                textState.onPressedBackspace()
            37 ->
                textState.onPressedLeftArrow()
            39 ->
                textState.onPressedRightArrow()
            else ->
                textState.onTypedChar(keyEvent.utf16CodePoint.toChar())
        }
    }
}
