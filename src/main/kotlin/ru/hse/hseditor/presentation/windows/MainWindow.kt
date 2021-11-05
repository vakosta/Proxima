package ru.hse.hseditor.presentation.windows

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
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.states.PanelState
import ru.hse.hseditor.presentation.utils.VerticalSplittable
import ru.hse.hseditor.presentation.views.CodeView
import ru.hse.hseditor.presentation.views.FileTreeView
import ru.hse.hseditor.presentation.views.Tab
import ru.hse.hseditor.presentation.views.dialogs.SwingFileDialog
import ru.hse.hseditor.presentation.views.dialogs.SwingFileDialogKind
import ru.hse.hseditor.presentation.views.dialogs.SwingAlertDialog
import ru.hse.hseditor.presentation.views.dialogs.SwingConfirmDialog
import javax.swing.JFileChooser
import kotlin.io.path.isDirectory

@Composable
fun MainWindow(state: MainWindowState, onCloseRequest: () -> Unit) = Window(
    title = "HSEditor",
    state = state,
    onCloseRequest = onCloseRequest,
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
}
@Composable
private fun FrameWindowScope.MainWindowMenuBar(state: MainWindowState) = MenuBar {
    val scope = rememberCoroutineScope()

    fun save() = scope.launch { /* save current document */ }
    fun openDirectory() = scope.launch { state.openDirectory() }
    fun exit() = Unit

    Menu("File") {
        Item("New tab", onClick = {})
        Item("Open folder...", onClick = { openDirectory() })
        Item("Save", onClick = {})
        Separator()
        Item("Exit", onClick = {})
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
