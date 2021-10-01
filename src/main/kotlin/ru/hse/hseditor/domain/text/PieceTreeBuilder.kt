package ru.hse.hseditor.domain.text

import ru.hse.hseditor.domain.text.file.FileChunk
import ru.hse.hseditor.domain.text.file.getLineStartOffsetsList

// So, remember how Java uses UTF-16 for ALL text?
// Yeah, it does, but when it encounters any BOM sequence in a UTF-encoded file
// it freaks out and reads it as the UTF-16 BE BOM, which is exactly FE FF.
// Java is a drunk and needs help.
internal const val UTF_BOM = '\uFEFF'

private fun String.hasUtfBom() = first() == UTF_BOM

class PieceTreeBuilder {
    private val myChunks = mutableListOf<FileChunk>()
    private val myLineStartOffsets = mutableListOf<Int>()

    fun acceptChunkString(str: String) {
        if (str.isEmpty()) return

        var debommedStr = str
        if (myChunks.isEmpty() && str.hasUtfBom()) {
            debommedStr = str.substring(1)
        }

//        val lastChar = debommedStr.last()
//        if (lastChar == '\r') {
//            debommedStr = debommedStr.substring(0, debommedStr.length - 1)
//        }

        myChunks.add(FileChunk(debommedStr, debommedStr.getLineStartOffsetsList()))
    }

    fun build(): PieceTree {
        // Normalize eof to /n
        for (i in myChunks.indices) {
            val normalizedStr = myChunks[i].chunk.replace(Regex("\r\n"), "\n")
            if (myChunks[i].chunk.length != normalizedStr.length) {
                val lineStarts = normalizedStr.getLineStartOffsetsList()
                myChunks[i] = FileChunk(normalizedStr, lineStarts)
            }
        }

        return PieceTree(myChunks, true)
    }

}
