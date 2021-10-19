package ru.hse.hseditor.domain.text

import ru.hse.hseditor.domain.text.file.getLineStartOffsetsList

internal const val UTF_BOM = '\uFEFF'

private fun String.hasUtfBom() = first() == UTF_BOM

class PieceTreeBuilder {
    private val myChunks = mutableListOf<FileChunk>()
    private val myLineStartOffsets = mutableListOf<Int>()

    fun acceptFileChunk(chunk: FileChunk) {
        if (chunk.isEmpty) return

        val debommedChunk = if (chunk.chunk.hasUtfBom()) {
            val str = chunk.chunkSubstring(1, chunk.chunkLength - 1)
            chunk.copy(chunk = str, lineStartOffsets = chunk.lineStartOffsets)
        } else chunk

        myChunks.add(debommedChunk)
    }

    fun acceptChunkString(str: String) {
        if (str.isEmpty()) return

        val debommedStr = if (myChunks.isEmpty() && str.hasUtfBom()) str.substring(1) else str

        myChunks.add(FileChunk(debommedStr, debommedStr.getLineStartOffsetsList()))
    }

    fun build(): PieceTree {
        // Normalize eol to \n
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
