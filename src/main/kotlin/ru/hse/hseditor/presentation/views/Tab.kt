package ru.hse.hseditor.presentation.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.hse.hseditor.presentation.states.EditorState

@Composable
fun Tab(
    state: EditorState,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Column(
        Modifier
            .width(IntrinsicSize.Max)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .background(color = getTabColor(state.isActive))
                .padding(all = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.fileName)
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource("icons/close.xml"),
                contentDescription = "",
                modifier = Modifier
                    .width(14.dp)
                    .height(14.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
            )
        }
        Box(
            modifier = Modifier
                .clip(RectangleShape)
                .height(3.dp)
                .fillMaxWidth()
                .background(color = getActiveRectangleColor(state.isActive))
        )
    }
}

private fun getTabColor(isActive: Boolean): Color =
    if (isActive)
        Color.White
    else
        Color(242, 242, 242)

private fun getActiveRectangleColor(isActive: Boolean): Color =
    if (isActive)
        Color(109, 171, 208)
    else
        Color.Transparent
