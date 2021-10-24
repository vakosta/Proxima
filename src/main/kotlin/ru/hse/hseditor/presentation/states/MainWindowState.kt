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
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.lifetimes.defineChildLifetime
import ru.hse.hseditor.domain.common.locks.runBlockingWrite
import ru.hse.hseditor.domain.common.vfs.mountVFSAtPathLifetimed
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.domain.skija.SkijaBuilder
import ru.hse.hseditor.domain.text.document.DocumentSource
import ru.hse.hseditor.presentation.model.FileModel
import ru.hse.hseditor.presentation.model.toFileModel
import java.nio.file.Paths
import java.time.Instant
import java.util.logging.Logger
import kotlin.io.path.absolute

class MainWindowState(
    private val myLifetime: Lifetime,
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
) : KoinComponent, WindowState {

//    private val fileSystemManager: FileSystemManager by inject()

    var renderedContent: ImageBitmap by mutableStateOf(
        SkijaBuilder("", 0, true, 300, 300).buildView()
    )

    val panelState: PanelState by mutableStateOf(PanelState())
    val fileTreeState: FileTreeViewModel = FileTreeViewModel(
        // TODO this need to be an option on the menu
        mountVFSAtPathLifetimed(myLifetime, Paths.get("").absolute()).root.toFileModel(),
        this::openEditor
    )
    val editorStates: MutableList<EditorState> = mutableStateListOf()
    var activeEditorState: EditorState?
        get() = editorStates.firstOrNull { it.isActive }
        set(value) {
            editorStates.forEach { it.isActive = false }
            value?.isActive = true
            updateRenderedContent()
        }

    private var typingTime = Instant.now()
    private var isShowCarriage = true

    init {
        // TODO extract a separete MainScope to a Lifetime
        tickerFlow(500).onEach { inverseCarriage() }.launchIn(MainScope())
    }

    private fun inverseCarriage() {
        if (Duration.between(typingTime, Instant.now()).seconds >= 1) {
            isShowCarriage = !isShowCarriage
            updateRenderedContent()
        }
    }

    private fun openEditor(file: FileModel) {
        if (file.vfsNode !is DocumentSource) return // TODO show modal

        val pieceTreeTextBuffer = file.vfsNode.makeDocument()

        val childLifetimeDef = defineChildLifetime(myLifetime, "${file.name} editor lifetime.")
        val editorState = EditorState(
            myLifetime = childLifetimeDef.lifetime,
            fileName = file.name,
            isActive = false,
            textState = TextState(
                myLifetime = childLifetimeDef.lifetime,
                language = TextState.Language.Kotlin,
                document = file.vfsNode.makeDocument(), // TODO async read
            )
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
        activeEditorState?.onKeyEvent(keyEvent) ?: return false
        val isKeyPassed = activeEditorState?.onKeyEvent(keyEvent)
        if (isKeyPassed == null || !isKeyPassed) {
            return false
        }
        typingTime = Instant.now()
        isShowCarriage = true
        LOG.info("Update content from event, thread ${Thread.currentThread().id}")
        updateRenderedContent()
        return true
    }

    fun updateRenderedContent(width: Int = renderedContent.width, height: Int = renderedContent.height) {
        // TODO: Start at EditorRange() and end at EditorRange(), will be faster
        val editor = activeEditorState ?: return
        runBlockingWrite {
            renderedContent = SkijaBuilder(
                content = editor.textState.document.getRawContent(),
                carriagePosition = editor.textState.carriageAbsoluteOffset,
                isShowCarriage = isShowCarriage,
                width = width,
                height = height,
                textState = editor.textState,
            ).buildView()
        }
    }

    companion object {
        val LOG = Logger.getLogger(MainWindowState::class.java.name)
    }
}
