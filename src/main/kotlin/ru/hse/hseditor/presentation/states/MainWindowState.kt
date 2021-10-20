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
import ru.hse.hseditor.domain.filesystem.FileSystemManager
import ru.hse.hseditor.domain.skija.SkijaBuilder
import ru.hse.hseditor.presentation.model.File

class MainWindowState(
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
    fileSystemManager: FileSystemManager = FileSystemManager(),
) : WindowState {

    val panelState by mutableStateOf(PanelState())
    val fileTreeState: FileTree = FileTree(fileSystemManager.getBaseDirectory(), this::openEditor)
    val editors: MutableList<EditorState> = mutableStateListOf()

    var fileContentRendered: ImageBitmap by mutableStateOf(
        SkijaBuilder("", 0, 300, 300).buildView()
    )

    fun setActiveEditor(editorState: EditorState) {
        editors.forEach { it.isActive = false }
        editorState.isActive = true
        updateRenderedContent()
    }

    private fun openEditor(file: File) {
        val editorState = EditorState(
            fileName = file.name,
            isActive = false,
        )
        editors.add(editorState)
        setActiveEditor(editorState)
    }

    fun closeEditor(editorState: EditorState) {
        if (editorState.isActive) {
            editors.firstOrNull { !it.isActive }?.isActive = true
        }
        editors.remove(editorState)
        updateRenderedContent()
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        editors.firstOrNull { it.isActive }?.onKeyEvent(keyEvent) ?: return false
        updateRenderedContent()
        return true
    }

    fun updateRenderedContent() {
        updateRenderedContent(fileContentRendered.width, fileContentRendered.height)
    }

    fun updateRenderedContent(width: Int, height: Int) {
        val editor = editors.firstOrNull { it.isActive } ?: return
        fileContentRendered = SkijaBuilder(
            editor.content.getLinesRawContent(),
            editor.carriagePosition,
            width,
            height
        ).buildView()
    }
}
