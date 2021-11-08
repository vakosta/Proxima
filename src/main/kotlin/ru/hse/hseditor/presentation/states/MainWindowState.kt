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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import ru.hse.hseditor.domain.common.ObservableList
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.lifetimes.LifetimeDef
import ru.hse.hseditor.domain.common.lifetimes.defineChildLifetime
import ru.hse.hseditor.domain.common.locks.runBlockingWrite
import ru.hse.hseditor.domain.common.vfs.OsVirtualFile
import ru.hse.hseditor.domain.common.vfs.OsVirtualFileSystem
import ru.hse.hseditor.domain.common.vfs.mountVFSAtPathLifetimed
import ru.hse.hseditor.domain.highlights.ExtModificationDesc
import ru.hse.hseditor.domain.highlights.ExtModificationKind
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.domain.skija.SkijaBuilder
import ru.hse.hseditor.domain.text.document.DocumentSource
import ru.hse.hseditor.presentation.model.FileModel
import ru.hse.hseditor.presentation.model.toFileModelLifetimed
import ru.hse.hseditor.presentation.views.MouseEvent
import ru.hse.hseditor.presentation.views.dialogs.SwingDialogResult
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.logging.Logger
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile
import kotlin.math.min
import kotlin.math.max
import kotlin.system.exitProcess

data class EditorStateDesc(val editorState: EditorState, val editorLifetimeDef: LifetimeDef)

class MainWindowState(
    private val myLifetime: Lifetime,
    override var placement: WindowPlacement = WindowPlacement.Floating,
    override var isMinimized: Boolean = false,
    override var position: WindowPosition = WindowPosition.PlatformDefault,
    override var size: WindowSize = WindowSize(800.dp, 600.dp),
    private val onCloseRequest: () -> Unit
) : KoinComponent, WindowState {

    val dialogs = DialogsController()
    val mainScope = MainScope()

    private var cursorX = 0F
    private var cursorY = 0F
    private var cursorState: MouseEvent = MouseEvent.RELEASE

    var renderedContent: ImageBitmap by mutableStateOf(SkijaBuilder().build())

    val panelState: PanelState by mutableStateOf(PanelState())
    var fileTreeState: FileTreeModel by mutableStateOf(
        FileTreeModel(
            FileModel("Nothing opened yet!", false, ObservableList(), false, null),
            this::openEditor
        )
    )

    val editorStateDescs: MutableList<EditorStateDesc> = mutableStateListOf()
    var activeEditorStateDesc: EditorStateDesc?
        get() = editorStateDescs.firstOrNull { it.editorState.isActive }
        set(value) {
            editorStateDescs.forEach { it.editorState.isActive = false }
            value?.editorState?.isActive = true
            updateRenderedContent()
        }

    private var typingTime = Instant.now()
    private var isShowCarriage = true

    init {
//        // TODO extract a separate MainScope to a Lifetime
//        tickerFlow(500).onEach { inverseCarriage() }.launchIn(mainScope)
    }

    private fun inverseCarriage() {
        if (Duration.between(typingTime, Instant.now()).seconds >= 1) {
            isShowCarriage = !isShowCarriage
            updateRenderedContent()
        }
    }

    private var myVfs: OsVirtualFileSystem? = null

    suspend fun openDirectory() {
        val dirPath = dialogs.openDirectory.awaitResult() ?: return
        fileTreeState = FileTreeModel(
            mountVFSAtPathLifetimed(myLifetime, dirPath.absolute())
                .apply { myVfs = this }
                .root.toFileModelLifetimed(myVfs?.fsLifetime ?: myLifetime),
            this::openEditor
        )
    }

    private suspend fun openEditor(file: FileModel) {
        if (file.vfsNode !is DocumentSource) return // TODO show modal

        val childLifetimeDef = defineChildLifetime(myLifetime, "${file.name} editor lifetime.")
        val editorState = EditorState(
            myLifetime = childLifetimeDef.lifetime,
            fileName = file.name,
            isActive = false,
            textState = TextState(
                myLifetime = childLifetimeDef.lifetime,
                language = TextState.Language.Kotlin,
                document = file.vfsNode.makeDocument() ?: return,
            )
        )
        val editorStateDesc = EditorStateDesc(editorState, childLifetimeDef)

        editorState.apply {

            fun handleExternalModification(
                it: ExtModificationDesc
            ) = runBlockingWrite { // Can arrive from any thread
                when (it.kind) {
                    ExtModificationKind.DOCUMENT_DISC_SYNC -> mainScope.launch {
                        when (dialogs.confirmFileUpdateFromDisc.awaitResult()) {
                            SwingDialogResult.YES -> {
                                textState.document.updateFromSource()
                                updateRenderedContent()
                            }
                            else -> textState.document.isSyncedWithDisc = false
                        }
                    }
                    ExtModificationKind.DOCUMENT_DELETED -> mainScope.launch {
                        dialogs.alertFileRemovedFromDisc.awaitResult()
                        textState.document.isSyncedWithDisc = false
                    }
                }
            }

            textState.externalModificationEvent.advise(childLifetimeDef.lifetime, ::handleExternalModification)
        }

        editorStateDescs.add(editorStateDesc)
        activeEditorStateDesc = editorStateDesc
    }

    fun closeEditor(editorState: EditorStateDesc) {
        if (editorState.editorState.isActive) {
            editorStateDescs.firstOrNull { !it.editorState.isActive }?.editorState?.isActive = true
        }
        editorState.editorLifetimeDef.terminateLifetime()
        editorStateDescs.remove(editorState)

        updateRenderedContent()
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        activeEditorStateDesc ?: return false
        val isKeyPassed = activeEditorStateDesc?.editorState?.onKeyEvent(keyEvent)
        if (isKeyPassed == null || !isKeyPassed) {
            return false
        }
        typingTime = Instant.now()
        isShowCarriage = true
        LOG.info("Update content from event, thread ${Thread.currentThread().id}")
        updateRenderedContent()
        return true
    }

    fun onPointChangeState(event: MouseEvent) {
        if (event == MouseEvent.PRESS) {
            activeEditorStateDesc?.editorState?.textState?.clearSelectionPositions()
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
        activeEditorStateDesc?.editorState?.setVerticalOffset(mouseScrollUnit.value * 20, renderedContent.height)
        updateRenderedContent()
    }

    fun updateRenderedContent(width: Int = renderedContent.width, height: Int = renderedContent.height) =
        synchronized(this) {
            // TODO: Start at EditorRange() and end at EditorRange(), will be faster
            val editorDesc = activeEditorStateDesc ?: return
            editorStateDescs.forEach { it.editorState.setVerticalOffset(0F, height) }
            val skijaBuilder = SkijaBuilder(
                content = editorDesc.editorState.textState.document.getRawContent(),
                caretPosition = editorDesc.editorState.textState.caretAbsoluteOffset,
                isShowCarriage = isShowCarriage,
                verticalScrollOffset = editorDesc.editorState.verticalOffset,
                horizontalScrollOffset = editorDesc.editorState.horizontalOffset,
                width = width,
                height = height,
                textState = editorDesc.editorState.textState,
                startSelectionPosition = min(
                    editorDesc.editorState.textState.firstSelectionPosition ?: -1,
                    editorDesc.editorState.textState.secondSelectionPosition ?: -1,
                ),
                endSelectionPosition = if (editorDesc.editorState.textState.secondSelectionPosition == null) {
                    -1
                } else {
                    max(
                        editorDesc.editorState.textState.firstSelectionPosition ?: -1,
                        editorDesc.editorState.textState.secondSelectionPosition ?: -1,
                    )
                },
            )
            renderedContent = skijaBuilder.build()
            editorDesc.editorState.maxTextX = skijaBuilder.maxTextX
            editorDesc.editorState.maxTextY = skijaBuilder.maxTextY
            editorDesc.editorState.charCoordinates = skijaBuilder.charCoordinates
        }

    companion object {
        val LOG = Logger.getLogger(MainWindowState::class.java.name)
    }

    private fun updateCaretPosition() {
        activeEditorStateDesc?.editorState?.updateCaretPosition(cursorX, cursorY) ?: return
        typingTime = Instant.now()
        isShowCarriage = true
        updateRenderedContent()
    }

    suspend fun saveActiveEditor() {
        val document = activeEditorStateDesc?.editorState?.textState?.document ?: return
        if (document.source != null) {
            document.writeToMainSource()
        } else {
            saveActiveEditorAs()
        }
    }

    private suspend fun makeNewVFile(): OsVirtualFile? {
        val path = dialogs.chooseFilePath.awaitResult()?.toAbsolutePath()
        if (path == null || path.parent == null || !path.isRegularFile()) {
            dialogs.illegalPath.awaitResult()
            return null
        }
        val directory = myVfs?.resolveDirAbsoluteOrNull(path.parent)
        if (directory == null) {
            dialogs.pathNotMounted.awaitResult()
            return null
        }
        val vFile = myVfs?.makeFile(directory.path, path)
        if (vFile == null) {
            dialogs.cantMakeFile.awaitResult()
            return null
        }
        return vFile
    }

    suspend fun saveActiveEditorAs() {
        val document = activeEditorStateDesc?.editorState?.textState?.document ?: return
        val vFile = makeNewVFile() ?: return

        document.writeToSecondarySource(vFile)
    }

    suspend fun interruptableExit() {
        val result = dialogs.confirmExit.awaitResult()
        if (result == SwingDialogResult.YES) {
            onCloseRequest()
            exitProcess(0)
        }
    }

    suspend fun createSourceAndOpenDocument() {
        val vFile = makeNewVFile() ?: return
        // Must be created by the power of ~REACTIVE~ programming after making a VFile
        val fileModel = fileTreeState.findByVfsNode(vFile) ?: return

        openEditor(fileModel)
    }

}

class DialogsController {
    val confirmExit = DialogState<SwingDialogResult>()
    val illegalPath = DialogState<SwingDialogResult>()
    val pathNotMounted = DialogState<SwingDialogResult>()
    val cantMakeFile = DialogState<SwingDialogResult>()

    val openDirectory = DialogState<Path?>()
    val chooseFilePath = DialogState<Path?>()

    val confirmFileUpdateFromDisc = DialogState<SwingDialogResult>()
    val alertFileRemovedFromDisc = DialogState<SwingDialogResult>()
}

class DialogState<T> {
    private var onResult: CompletableDeferred<T>? by mutableStateOf(null)

    val isAwaiting get() = onResult != null

    suspend fun awaitResult(): T {
        onResult = CompletableDeferred()
        val result = onResult!!.await()
        onResult = null
        return result
    }

    fun onResult(result: T) = onResult!!.complete(result)
}
