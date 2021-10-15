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
import ru.hse.hseditor.domain.skija.SkijaBuilder
import ru.hse.hseditor.presentation.model.File
import ru.hse.hseditor.presentation.model.getFile

class MainWindowState(
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
) : WindowState {

    val panelState by mutableStateOf(PanelState())

    val fileTreeState: FileTree =
        FileTree(File("Kek", true, listOf(getFile(), getFile(), getFile()), true))
    val editors: MutableList<EditorState> = mutableStateListOf(
        EditorState("Kek.kt", false),
        EditorState("MainWindowState.kt", true),
        EditorState("MainWindowNeState.kt", false),
        EditorState("LolKekCheburek.java", false),
    )

    var fileContentRendered: ImageBitmap by mutableStateOf(
        SkijaBuilder("", 0, 300, 300).buildView()
    )

    fun setActiveEditor(editorState: EditorState) {
        editors.forEach { it.isActive = false }
        editorState.isActive = true
        updateRenderedContent()
    }

    fun closeEditor(editorState: EditorState) {
        if (editorState.isActive) {
            editors.firstOrNull { !it.isActive }?.isActive = true
        }
        editors.remove(editorState)
        updateRenderedContent()
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        editors.first { it.isActive }.onKeyEvent(keyEvent)
        updateRenderedContent()
        return true
    }

    fun updateRenderedContent() {
        updateRenderedContent(fileContentRendered.width, fileContentRendered.height)
    }

    fun updateRenderedContent(width: Int, height: Int) {
        val editor = editors.first { it.isActive }
        fileContentRendered = SkijaBuilder(
            editor.content.getLinesRawContent(),
            editor.carriagePosition,
            width,
            height
        ).buildView()
    }
}
