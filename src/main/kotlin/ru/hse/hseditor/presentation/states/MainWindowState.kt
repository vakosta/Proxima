package ru.hse.hseditor.presentation.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.hse.hseditor.domain.app.lifetimes.Lifetime
import ru.hse.hseditor.domain.app.tickerFlow
import ru.hse.hseditor.domain.filesystem.FileSystemManager
import ru.hse.hseditor.domain.skija.SkijaBuilder
import ru.hse.hseditor.presentation.model.File
import ru.hse.hseditor.presentation.views.MouseEvent
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

class MainWindowState(
    private val myLifetime: Lifetime,
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
) : KoinComponent, WindowState {

    private val fileSystemManager: FileSystemManager by inject()

    private var cursorX = 0F
    private var cursorY = 0F
    private var cursorState: MouseEvent = MouseEvent.RELEASE

    var renderedContent: ImageBitmap by mutableStateOf(SkijaBuilder().build())

    val panelState: PanelState by mutableStateOf(PanelState())
    val fileTreeState: FileTree = FileTree(fileSystemManager.getBaseDirectory(), this::openEditor)
    val editorStates: MutableList<EditorState> = mutableStateListOf()
    var activeEditorState: EditorState?
        get() = editorStates.firstOrNull { it.isActive }
        set(value) {
            editorStates.forEach { it.isActive = false }
            value?.isActive = true
            updateRenderedContent()
        }

    @Volatile private var typingTime = Instant.now()
    @Volatile private var isShowCarriage = true

    init {
        // TODO extract a separete MainScope to a Lifetime
        tickerFlow(500).onEach { inverseCarriage() }.launchIn(MainScope())
    }

    fun openEditor(file: File) {
        val editorState = EditorState(
            fileName = file.name,
            isActive = false,
        )
        editorStates.add(editorState)
        activeEditorState = editorState
    }

    fun closeEditor(editorState: EditorState) {
        if (editorState.isActive) {
            editorStates.firstOrNull { !it.isActive }?.isActive = true
        }
        editorStates.remove(editorState)
        updateRenderedContent()
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        val isKeyPassed = activeEditorState?.onKeyEvent(keyEvent)
        if (isKeyPassed == null || !isKeyPassed) {
            return false
        }
        typingTime = Instant.now()
        isShowCarriage = true
        updateRenderedContent()
        return true
    }

    fun onPointChangeState(event: MouseEvent) {
        if (event == MouseEvent.PRESS) {
            activeEditorState?.textState?.clearSelectionPositions()
        }
        cursorState = event
        updateCaretPosition()
    }

    fun onPointMove(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        if (cursorState == MouseEvent.PRESS) {
            updateCaretPosition()
        }
    }

    fun onScroll(mouseScrollUnit: MouseScrollUnit.Line) {
        activeEditorState?.setVerticalOffset(mouseScrollUnit.value * 20, renderedContent.height)
        updateRenderedContent()
    }

    fun updateRenderedContent(width: Int = renderedContent.width, height: Int = renderedContent.height) {
        // TODO: Start at EditorRange() and end at EditorRange(), will be faster
        val editor = activeEditorState ?: return
        editorStates.forEach { it.setVerticalOffset(0F, height) }
        val skijaBuilder = SkijaBuilder(
            content = editor.textState.pieceTree.getLinesRawContent(),
            caretPosition = editor.textState.caretAbsoluteOffset,
            isShowCarriage = isShowCarriage,
            verticalScrollOffset = editor.verticalOffset,
            horizontalScrollOffset = editor.horizontalOffset,
            width = width,
            height = height,
            textState = editor.textState,
            startSelectionPosition = min(
                editor.textState.firstSelectionPosition ?: -1,
                editor.textState.secondSelectionPosition ?: -1,
            ),
            endSelectionPosition = if (editor.textState.secondSelectionPosition == null) {
                -1
            } else {
                max(
                    editor.textState.firstSelectionPosition ?: -1,
                    editor.textState.secondSelectionPosition ?: -1,
                )
            },
        )
        renderedContent = skijaBuilder.build()
        editor.maxTextX = skijaBuilder.maxTextX
        editor.maxTextY = skijaBuilder.maxTextY
        editor.charCoordinates = skijaBuilder.charCoordinates
    }

    private fun inverseCarriage() {
        if (Duration.between(typingTime, Instant.now()).seconds >= 1) {
            isShowCarriage = !isShowCarriage
            updateRenderedContent()
        }
    }

    private fun updateCaretPosition() {
        activeEditorState?.updateCaretPosition(cursorX, cursorY) ?: return
        typingTime = Instant.now()
        isShowCarriage = true
        updateRenderedContent()
    }
}
