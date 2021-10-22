package ru.hse.hseditor.domain.app.vfs

import ru.hse.hseditor.domain.text.FileChunk
import ru.hse.hseditor.domain.text.PREFERRED_PIECE_TREE_CHUNK_SIZE
import ru.hse.hseditor.domain.text.PieceTreeBuilder
import ru.hse.hseditor.domain.text.document.Document
import ru.hse.hseditor.domain.text.document.DocumentSource
import ru.hse.hseditor.domain.text.document.PieceTreeDocument
import ru.hse.hseditor.domain.text.file.getLineStartOffsetsList
import java.nio.charset.Charset
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader
import kotlin.io.path.writeLines

private val listEmpty = listOf<VFSNode>()

sealed interface VFSNode {
    val parent: VFSNode?
    val children: List<VFSNode>
}

sealed interface OsVFSNode : VFSNode {
    val fileSystem: OsVirtualFileSystem
    val path: Path
}

private fun fetchFileChunksSequence(absolutePath: Path) = sequence {
    require(absolutePath.isAbsolute) { "Cannot open a file from a relative path!" }

    val charArr = CharArray(PREFERRED_PIECE_TREE_CHUNK_SIZE)
    var readCharsNum: Int

    absolutePath.bufferedReader().use {
        while (it.ready()) {
            readCharsNum = it.read(charArr)
            val str = String(charArr)
            yield(FileChunk(str, str.getLineStartOffsetsList()))
            if (readCharsNum < PREFERRED_PIECE_TREE_CHUNK_SIZE) break
        }
    }
}

class OsVirtualFile(
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode, DocumentSource() {
    override val children get() = listEmpty

    override fun makeDocument(): Document {
        val pieceTreeBuilder = PieceTreeBuilder()
        fetchFileChunksSequence(fileSystem.absoluteRootPath.resolve(path))
            .forEach { pieceTreeBuilder.acceptFileChunk(it) }

        myDocument = PieceTreeDocument(pieceTreeBuilder.build())
        return myDocument
    }

    override fun commitDocument() {
        // TODO we can do diffs here, but it would probably require some sort of
        // TODO file indexing, so... no diffs, many excess calculations!
        // TODO this need testing...
        fileSystem.absoluteRootPath.resolve(path).writeLines(
            myDocument.fetchLinesSequence(1..myDocument.lineCount),
            Charset.defaultCharset(),
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    override fun refreshDocument() {

    }
}

class OsVirtualDirectory(
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
): OsVFSNode {
    internal val mutChildren = mutableListOf<OsVFSNode>()
    override val children: List<OsVFSNode> get() = mutChildren

    fun resolveChildNodeOrNull(path: Path) = children.firstOrNull { path.startsWith(it.path) }
}

class OsVirtualSymlink(
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode {
    override val children get() = listEmpty
}
