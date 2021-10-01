package ru.hse.hseditor.presentation.utils

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

fun KeyEvent.isRelevant(): Boolean =
    this.type == KeyEventType.KeyDown && (this.utf16CodePoint != 65535
            || this.nativeKeyEvent.keyCode == 37  // Left arrow
            || this.nativeKeyEvent.keyCode == 38  // Up arrow
            || this.nativeKeyEvent.keyCode == 39  // Right arrow
            || this.nativeKeyEvent.keyCode == 40) // Down arrow
