package me.vakosta.proxima.presentation.windows

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.vakosta.proxima.presentation.states.MainWindowState
import me.vakosta.proxima.presentation.states.PanelState
import me.vakosta.proxima.presentation.utils.VerticalSplittable
import me.vakosta.proxima.presentation.views.CodeView
import me.vakosta.proxima.presentation.views.FileTreeView
import me.vakosta.proxima.presentation.views.Tab
import me.vakosta.proxima.presentation.views.dialogs.SwingAlertDialog
import me.vakosta.proxima.presentation.views.dialogs.SwingConfirmDialog
import me.vakosta.proxima.presentation.views.dialogs.SwingFileDialog
import me.vakosta.proxima.presentation.views.dialogs.SwingFileDialogKind
import javax.swing.JFileChooser
import kotlin.io.path.isDirectory

@Composable
fun MainWindow(state: MainWindowState) = Window(
    title = "HSEditor",
    state = state,
    onCloseRequest = { runBlocking { state.interruptableExit() } },
    onKeyEvent = { state.onKeyEvent(it) }
) {
    val animatedSize = if (state.panelState.splitter.isResizing) {
        state.panelState.size
    } else {
        animateDpAsState(
            state.panelState.size,
            SpringSpec(stiffness = Spring.StiffnessLow)
        ).value
    }

    MainWindowMenuBar(state)

    VerticalSplittable(
        modifier = Modifier.fillMaxSize(),
        splitterState = state.panelState.splitter,
        onResize = {
            state.panelState.expandedSize =
                    (state.panelState.expandedSize + it).coerceAtLeast(state.panelState.expandedSizeMin)
        },
    ) {
        ResizablePanel(Modifier.width(animatedSize).fillMaxHeight(), state.panelState) {
            FileTreeView(state.fileTreeState)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = Color(209, 209, 209))
            ) {
                items(state.editorStateDescs) { editorStateDesc ->
                    Tab(
                        state = editorStateDesc.editorState,
                        onClick = { state.activeEditorStateDesc = editorStateDesc },
                        onClose = { state.closeEditor(editorStateDesc) },
                    )
                }
            }
            CodeView(
                isVisible = state.editorStateDescs.isNotEmpty(),
                code = state.renderedContent,
                onPointMove = state::onPointMove,
                onPointChangeState = state::onPointChangeState,
                onScroll = state::onScroll,
                onGloballyPositioned = { state.updateRenderedContent(it.size.width, it.size.height) }
            )
        }
    }

    //region Dialogs

    if (state.dialogs.openDirectory.isAwaiting) {
        SwingFileDialog(
            "Open directory...",
            SwingFileDialogKind.OPEN,
            onPathChosen = {
                if (it?.isDirectory() == true) {
                    state.dialogs.openDirectory.onResult(it)
                }
            },
            fileSelectionModeInt = JFileChooser.DIRECTORIES_ONLY
        )
    }

    if (state.dialogs.confirmFileUpdateFromDisc.isAwaiting) {
        SwingConfirmDialog(
            "File updated on disc",
            "Files were updated on disc. Do you wish to reload them?",
            onOptionChosen = { state.dialogs.confirmFileUpdateFromDisc.onResult(it) }
        )
    }

    if (state.dialogs.alertFileRemovedFromDisc.isAwaiting) {
        SwingAlertDialog(
            "File removed from disc",
            "Some active file was removed from disc. It's editor will be closed. Sorry!",
            onOptionChosen = { state.dialogs.alertFileRemovedFromDisc.onResult(it) }
        )
    }

    if (state.dialogs.cantMakeFile.isAwaiting) {
        SwingAlertDialog(
            "Can't make a file!",
            "Can't make a virtual file with a specified path!",
            onOptionChosen = { state.dialogs.cantMakeFile.onResult(it) }
        )
    }

    if (state.dialogs.confirmExit.isAwaiting) {
        SwingConfirmDialog(
            "Exit",
            "Do you really want to exit?",
            onOptionChosen = { state.dialogs.confirmExit.onResult(it) }
        )
    }

    if (state.dialogs.chooseFilePath.isAwaiting) {
        SwingFileDialog(
            "Open a file...",
            SwingFileDialogKind.OPEN,
            onPathChosen = {
                state.dialogs.chooseFilePath.onResult(it)
            },
            fileSelectionModeInt = JFileChooser.FILES_AND_DIRECTORIES
        )
    }

    if (state.dialogs.illegalPath.isAwaiting) {
        SwingAlertDialog(
            "Illegal path",
            "The chosen path is illegal for a file! Please, choose a file!",
            onOptionChosen = { state.dialogs.illegalPath.onResult(it) }
        )
    }

    if (state.dialogs.pathNotMounted.isAwaiting) {
        SwingAlertDialog(
            "Path not mounted",
            "Path is not in the project model, can't create a file. " +
                    "The author didn't want to add support for external virtual files. What a bummer.",
            onOptionChosen = { state.dialogs.pathNotMounted.onResult(it) }
        )
    }

    //endregion

}

@Composable
private fun FrameWindowScope.MainWindowMenuBar(state: MainWindowState) = MenuBar {
    val scope = rememberCoroutineScope()

    fun save() = scope.launch { state.saveActiveEditor() }
    fun saveAs() = scope.launch { state.saveActiveEditorAs() }

    fun openDirectory() = scope.launch { state.openDirectory() }
    fun exit() = scope.launch { state.interruptableExit() }

    fun newTab() = scope.launch { state.createSourceAndOpenDocument() }

    Menu("File") {
        Item("New file with tab", onClick = { newTab() })
        Item("Open folder...", onClick = { openDirectory() })
        Item("Save", onClick = { save() })
        Item("Save as...", onClick = { saveAs() })
        Separator()
        Item("Exit", onClick = { exit() })
    }
}

@Composable
private fun ResizablePanel(
    modifier: Modifier,
    state: PanelState,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        if (state.isExpanded) 1f else 0f,
        SpringSpec(stiffness = Spring.StiffnessLow)
    )

    Box(modifier) {
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha)) {
            content()
        }

        Icon(
            if (state.isExpanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
            contentDescription = if (state.isExpanded) "Collapse" else "Expand",
            tint = LocalContentColor.current,
            modifier = Modifier
                .padding(top = 4.dp)
                .width(24.dp)
                .clickable {
                    state.isExpanded = !state.isExpanded
                }
                .padding(4.dp)
                .align(Alignment.TopEnd)
        )
    }
}
