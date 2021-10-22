package ru.hse.hseditor.presentation.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.hse.hseditor.domain.app.lifetimes.Lifetime
import ru.hse.hseditor.domain.app.tickerFlow
import ru.hse.hseditor.domain.filesystem.FileSystemManager
import ru.hse.hseditor.domain.skija.SkijaBuilder
import ru.hse.hseditor.presentation.model.File
import java.time.Instant
import kotlin.concurrent.thread

class MainWindowState(
    private val myLifetime: Lifetime,
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
) : WindowState, KoinComponent {

    private val fileSystemManager: FileSystemManager by inject()

    var renderedContent: ImageBitmap by mutableStateOf(
        SkijaBuilder("", 0, true, 300, 300).buildView()
    )

    val panelState: PanelState by mutableStateOf(PanelState())
    val fileTreeState: FileTree = FileTree(fileSystemManager.getBaseDirectory(), this::openEditor)
    val editorStates: MutableList<EditorState> = mutableStateListOf()

    @Volatile private var typingTime = Instant.now()
    @Volatile private var isShowCarriage = true

    init {
        // TODO extract a separete MainScope to a Lifetime
        tickerFlow(500).onEach { /* Update render (maybe partial?) */ }.launchIn(MainScope())
    }

    fun setActiveEditor(editorState: EditorState) {
        editorStates.forEach { it.isActive = false }
        editorState.isActive = true
        updateRenderedContent()
    }

    private fun openEditor(file: File) {
        val editorState = EditorState(
            fileName = file.name,
            isActive = false,
        )
        editorStates.add(editorState)
        setActiveEditor(editorState)
    }

    fun closeEditor(editorState: EditorState) {
        if (editorState.isActive) {
            editorStates.firstOrNull { !it.isActive }?.isActive = true
        }
        editorStates.remove(editorState)
        updateRenderedContent()
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        editorStates.firstOrNull { it.isActive }?.onKeyEvent(keyEvent) ?: return false
        typingTime = Instant.now()
        isShowCarriage = true
        updateRenderedContent()
        return true
    }

    fun updateRenderedContent() {
        updateRenderedContent(renderedContent.width, renderedContent.height)
    }

    fun updateRenderedContent(width: Int, height: Int) {
        // Start at EditorRange() and end at EditorRange(), will be faster
        val editor = editorStates.firstOrNull { it.isActive } ?: return
        renderedContent = SkijaBuilder(
            editor.content.getLinesRawContent(),
            editor.carriagePosition,
            isShowCarriage,
            width,
            height,
        ).buildView()
    }
}
