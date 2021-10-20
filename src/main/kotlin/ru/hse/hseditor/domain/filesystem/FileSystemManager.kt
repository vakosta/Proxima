package ru.hse.hseditor.domain.filesystem

import ru.hse.hseditor.presentation.model.File
import ru.hse.hseditor.presentation.model.getFile

class FileSystemManager {

    fun getBaseDirectory(): File = File(
        "Kek",
        true,
        listOf(
            getFile("Lol.kt"),
            getFile(),
            getFile("Cheburek.kt"),
        ),
        true,
    )

    private fun getFiles(basePath: String): List<File> {
        return listOf()
    }
}
