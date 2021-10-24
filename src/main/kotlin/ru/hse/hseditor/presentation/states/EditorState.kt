package ru.hse.hseditor.presentation.states

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.presentation.utils.isRelevant
import kotlin.math.max
import kotlin.math.min

class EditorState(
    var fileName: String,
    var isActive: Boolean = false,
) : KoinComponent {

    val textState: TextState by inject { parametersOf("", TextState.Language.Kotlin) }

    var verticalOffset: Float = 0F
        private set
    var horizontalOffset: Float = 0F
        private set

    var maxTextX = 0F
    var maxTextY = 0F

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.isRelevant()) {
            handleKeyEvent(keyEvent)
            return true
        }
        return false
    }

    fun onVerticalOffset(delta: Float, windowHeight: Int) {
        val scrollMax = max(0F, (maxTextY + 20F) - windowHeight)
        verticalOffset = min(scrollMax, max(0F, verticalOffset + delta))
    }

    private fun handleKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.nativeKeyEvent.keyCode) {
            8 ->
                textState.onPressedBackspace()
            37 ->
                textState.onPressedLeftArrow()
            38 ->
                textState.onPressedUpArrow()
            39 ->
                textState.onPressedRightArrow()
            40 ->
                textState.onPressedDownArrow()
            else ->
                textState.onTypedChar(keyEvent.utf16CodePoint.toChar())
        }
    }
}
