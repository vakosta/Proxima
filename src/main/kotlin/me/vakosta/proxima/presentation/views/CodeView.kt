package me.vakosta.proxima.presentation.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.vakosta.proxima.presentation.utils.cursorForText

enum class MouseButton {
    LEFT,
    MIDDLE,
    RIGHT,
}

enum class MouseEvent {
    PRESS,
    RELEASE,
}

@Composable
fun CodeView(
    isVisible: Boolean,
    code: ImageBitmap,
    onPointMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    onPointChangeState: (mouseEvent: MouseEvent) -> Unit = {},
    onScroll: (delta: MouseScrollUnit.Line) -> Unit = {},
    onGloballyPositioned: (onGloballyPositioned: LayoutCoordinates) -> Unit = {},
) {

    val width = rememberSaveable { mutableStateOf(1) }
    val height = rememberSaveable { mutableStateOf(1) }

    if (isVisible) {
        Image(
            bitmap = code,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart,
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
                .pointerMoveFilter(onMove = { pointerMove(onPointMove, it) })
                .pointerInput(Unit) { pointerInput(onPointChangeState) }
                .mouseScrollFilter { event, bounds -> mouseScrollFilter(onScroll, event, bounds) }
                .onGloballyPositioned { onGloballyPositioned(it, width, height, onGloballyPositioned) }
                .cursorForText(),
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().background(color = Color(java.awt.Color.decode("#cccccc").rgb)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource("icons/file_document.xml"),
                contentDescription = "",
                alignment = Alignment.Center,
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp),
            )
            Text("Откройте файл для работы", color = Color(java.awt.Color.decode("#747474").rgb))
        }
    }
}

private fun pointerMove(
    onPointMove: (x: Float, y: Float) -> Unit,
    it: Offset,
): Boolean {
    onPointMove(it.x, it.y)
    return false
}

private suspend fun PointerInputScope.pointerInput(
    onPointChangeState: (mouseEvent: MouseEvent) -> Unit,
) {
    while (true) {
        awaitPointerEventScope {
            val pointerEvent = awaitPointerEvent()
            pointerEvent.changes.forEach {
                val button = when (pointerEvent.mouseEvent?.button) {
                    1 -> MouseButton.LEFT
                    2 -> MouseButton.MIDDLE
                    3 -> MouseButton.RIGHT
                    else -> null
                }
                if (button != null && it.pressed && !it.previousPressed) {
                    onPointChangeState(MouseEvent.PRESS)
                    it.consumeDownChange()
                } else if (button != null && !it.pressed && it.previousPressed) {
                    onPointChangeState(MouseEvent.RELEASE)
                    it.consumeDownChange()
                }
            }
        }
    }
}

private fun mouseScrollFilter(
    onScroll: (delta: MouseScrollUnit.Line) -> Unit,
    event: MouseScrollEvent,
    bounds: IntSize,
): Boolean {
    onScroll(event.delta as MouseScrollUnit.Line)
    bounds.width
    return true
}

private fun onGloballyPositioned(
    it: LayoutCoordinates,
    width: MutableState<Int>,
    height: MutableState<Int>,
    onGloballyPositioned: (onGloballyPositioned: LayoutCoordinates) -> Unit,
) {
    if (it.size.width != width.value || it.size.height != height.value) {
        width.value = it.size.width
        height.value = it.size.height
        onGloballyPositioned(it)
    }
}
