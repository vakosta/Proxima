package domain.text.file

import domain.text.PREFERRED_PIECE_TREE_CHUNK_SIZE
import java.io.File

// This will get scrapped and rewritten when VFS comes around.
// !!!TEMP!!!

interface CharacterChunk {
    val chunk: String
    var lineStartOffsets: List<Int>

    fun chunkSubstring(start: Int, end: Int): String
}

/**
 * An immutable chunk of original file text
 */
data class FileChunk(
    override val chunk: String,
    override var lineStartOffsets: List<Int>
) : CharacterChunk {
    override fun chunkSubstring(start: Int, end: Int) = chunk.substring(start, end)
}

/**
 * A mutable chunk that receives all the new text added to the PieceTree
 * via it's [commit] method.
 */
data class DiffChunk(
    private val diffBuilder: StringBuilder,
    private val myLineStartOffsets: MutableList<Int>
) : CharacterChunk {

    override var lineStartOffsets: List<Int> = myLineStartOffsets
    override val chunk get() = diffBuilder.toString()

    val chunkLength get() = diffBuilder.length

    fun commit(str: String) {
        diffBuilder.append(str)
    }

    override fun chunkSubstring(start: Int, end: Int) = diffBuilder.substring(start, end) ?: ""
}

fun fetchPieceTreeFileChunks(fileName: String): List<FileChunk> {
    val charArr = CharArray(PREFERRED_PIECE_TREE_CHUNK_SIZE)
    var readCharsNum: Int
    val result = mutableListOf<FileChunk>()

    File(fileName).bufferedReader().use {
        while (it.ready()) {
            readCharsNum = it.read(charArr)
            val str = String(charArr)
            result.add(FileChunk(str, str.getLineStartOffsetsList()))
            if (readCharsNum < PREFERRED_PIECE_TREE_CHUNK_SIZE) break
        }
    }

    return result
}

/**
 * Get the list of line start indices starting at 0 (string start)
 */
fun String.getLineStartOffsetsList(): MutableList<Int> {
    val result = mutableListOf(0)
    // Because var on loop parameters isn't allowed.
    // Kotlin is nice, but sometimes it's a monstrosity.
    var i = 0
    while (i < this.length) {
        when (this[i]) {
            '\r' -> if (i + 1 < this.length && this[i + 1] == '\n') {
                // got \r\n
                result.add(i + 2)
                ++i
            }
            else {
                // got \r
                result.add(i + 1)
            }
            '\n' -> result.add(i + 1)
        }

        ++i
    }
    return result
}