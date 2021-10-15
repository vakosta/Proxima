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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.states.PanelState
import ru.hse.hseditor.presentation.utils.VerticalSplittable
import ru.hse.hseditor.presentation.views.CodeView
import ru.hse.hseditor.presentation.views.FileTreeView
import ru.hse.hseditor.presentation.views.Tab
import kotlin.system.exitProcess

@Composable
fun MainWindow(state: MainWindowState) = Window(
    state = state,
    onCloseRequest = { exitProcess(0) /* TODO some other handling obv required */ },
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
                items(state.editors) { editorState ->
                    Tab(
                        state = editorState,
                        onClick = { state.setActiveEditor(editorState) },
                        onClose = { state.closeEditor(editorState) },
                    )
                }
            }
            CodeView(
                code = state.fileContentRendered,
                onGloballyPositioned = {
                    state.updateRenderedContent(it.size.width, it.size.height)
                }
            )
        }
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
