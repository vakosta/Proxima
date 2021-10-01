package ru.hse.hseditor.presentation.model

import kotlinx.coroutines.CoroutineScope

class File(
    val name: String,
    val isDirectory: Boolean,
    val children: List<File>,
    val hasChildren: Boolean
) {
    fun readLines(scope: CoroutineScope): TextLines {
        return BaseTextLines
    }
}

fun getFile(): File =
    File("Kek", false, listOf(), false)
