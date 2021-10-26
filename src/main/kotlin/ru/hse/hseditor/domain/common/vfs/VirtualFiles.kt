package ru.hse.hseditor.domain.common.vfs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import ru.hse.hseditor.domain.common.Event
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.text.FileChunk
import ru.hse.hseditor.domain.text.PREFERRED_PIECE_TREE_CHUNK_SIZE
import ru.hse.hseditor.domain.text.PieceTreeBuilder
import ru.hse.hseditor.domain.text.document.Document
import ru.hse.hseditor.domain.text.document.DocumentSource
import ru.hse.hseditor.domain.text.document.PieceTreeDocument
import ru.hse.hseditor.domain.text.file.getLineStartOffsetsList
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.Charset
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.concurrent.thread
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.writeLines

private val listEmpty = listOf<VFSNode>()

sealed interface VFSNode {
    val fsLifetime: Lifetime
    val parent: VFSNode?
    val children: List<VFSNode>
}

sealed interface OsVFSNode : VFSNode {
    val changedEvent: Event<Unit>
    val fileSystem: OsVirtualFileSystem
    val path: Path
}

private fun fetchFileChunksFlow(absolutePath: Path) = flow {
    require(absolutePath.isAbsolute) { "Cannot open a file from a relative path!" }

    val charArr = CharArray(PREFERRED_PIECE_TREE_CHUNK_SIZE)
    var readCharsNum: Int

    absolutePath.bufferedReader().use {
        while (it.ready()) {
            // TODO @thisisvolatile it does 3 copies, and i dunno how to fix it.
            readCharsNum = it.read(charArr)
            val str = buildString { append(charArr, 0, readCharsNum) }
            emit(FileChunk(str, str.getLineStartOffsetsList()))
            if (readCharsNum < PREFERRED_PIECE_TREE_CHUNK_SIZE) break
        }
    }
}

class OsVirtualFile(
    override val fsLifetime: Lifetime,
    override val parent: OsVirtualDirectory?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode, DocumentSource() {
    override val changedEvent = Event<Unit>("OsVirtualFile::changedEvent")
    override val children get() = listEmpty

    override suspend fun makeDocumentSuspend(): Document? {
        parent ?: return null
        val scope = CoroutineScope(Dispatchers.IO)
        val pieceTreeBuilder = PieceTreeBuilder()
        fetchFileChunksFlow(parent.path.resolve(path))
            .map { pieceTreeBuilder.acceptFileChunk(it) }
            .launchIn(scope).join()

        val document = PieceTreeDocument(this, pieceTreeBuilder.build())
        openedDocuments.add(document)
        return document
    }

    override fun commitDocument(document: Document) {
        require(document in openedDocuments) { "Document from some other source cannot be commited" }
        // TODO we can do diffs here, but it would probably require some sort of
        // TODO file indexing, so... no diffs, many excess calculations!
        fileSystem.absoluteRootPath.resolve(path).bufferedWriter(
            Charset.defaultCharset(),
            32,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { br ->
            document.lines().forEach { br.write(it) }
        }
    }

    override fun refreshDocument(document: Document) {

    }
}

class OsVirtualDirectory(
    override val fsLifetime: Lifetime,
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode {
    internal val mutChildren = mutableListOf<OsVFSNode>()
    override val changedEvent = Event<Unit>("OsVirtualDirectory::changedEvent")
    override val children: List<OsVFSNode> get() = mutChildren

    fun resolveChildNodeOrNull(absolutePath: Path): OsVFSNode? {
        require(absolutePath.isAbsolute) { "Path needs to be absolute!" }
        return children.firstOrNull { absolutePath.startsWith(it.path) }
    }

    val beforeChildAddRemoveEvent = Event<NodeChangingDescriptor>("OsVirtualDirectory::beforeChildAddRemoveEvent")

    fun addChildFiring(toAdd: OsVFSNode) {
        beforeChildAddRemoveEvent.fire(NodeChangingDescriptor(NodeChangeKind.ADD, toAdd))
        mutChildren.add(toAdd)
    }

    fun removeChildFiring(toRemove: OsVFSNode) {
        beforeChildAddRemoveEvent.fire(NodeChangingDescriptor(NodeChangeKind.REMOVE, toRemove))
        mutChildren.remove(toRemove)
    }
}

class OsVirtualSymlink(
    override val fsLifetime: Lifetime,
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode {
    override val changedEvent = Event<Unit>("OsVirtualSymlink::changedEvent")
    override val children get() = listEmpty
}
