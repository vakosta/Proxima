package ru.hse.hseditor.presentation.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun CodeView(
    code: ImageBitmap,
    onGloballyPositioned: (onGloballyPositioned: LayoutCoordinates) -> Unit = {},
) {

    androidx.compose.foundation.Image(
        bitmap = code,
        contentDescription = "",
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopStart,
        modifier = Modifier.onGloballyPositioned { onGloballyPositioned(it) }
    )
}
