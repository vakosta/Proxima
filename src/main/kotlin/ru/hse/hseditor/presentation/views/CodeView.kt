package ru.hse.hseditor.presentation.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun CodeView(
    isVisible: Boolean,
    code: ImageBitmap,
    onGloballyPositioned: (onGloballyPositioned: LayoutCoordinates) -> Unit = {},
) {

    if (isVisible) {
        Image(
            bitmap = code,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { onGloballyPositioned(it) },
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
