package ru.hse.hseditor.domain.common.vfs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import ru.hse.hseditor.domain.common.Event
import ru.hse.hseditor.domain.common.ObservableSet
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.text.FileChunk
import ru.hse.hseditor.domain.text.PREFERRED_PIECE_TREE_CHUNK_SIZE
import ru.hse.hseditor.domain.text.PieceTreeBuilder
import ru.hse.hseditor.domain.text.document.Document
import ru.hse.hseditor.domain.text.document.DocumentSource
import ru.hse.hseditor.domain.text.document.PieceTreeDocument
import ru.hse.hseditor.domain.text.file.getLineStartOffsetsList
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter

private val setEmpty = setOf<VFSNode>()

sealed interface VFSNode {
    val fsLifetime: Lifetime
    val parent: VFSNode?
    val children: Set<VFSNode>
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
    override val children get() = setEmpty

    override suspend fun makeDocument(): Document {
        val scope = CoroutineScope(Dispatchers.IO)
        val pieceTreeBuilder = PieceTreeBuilder()
        fetchFileChunksFlow(parent?.path?.resolve(path) ?: path)
            .map { pieceTreeBuilder.acceptFileChunk(it) }
            .launchIn(scope).join()

        val document = PieceTreeDocument(this, pieceTreeBuilder.build())
        openedDocuments.add(document)
        return document
    }

    override suspend fun rememberOpenedDocument(document: Document) {
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

        // Handle sync
        // TODO more sophisticated sync handling???
        openedDocuments.forEach { if (it != document) document.isSyncedWithDisc = false }
        document.isSyncedWithDisc = true
    }

    override suspend fun refreshOpenedDocument(document: Document) {
        require(document in openedDocuments) { "Document from some other source cannot be commited" }
        document.textState.document = makeDocument()

        // Handle sync
        document.isSyncedWithDisc = true
    }

    override suspend fun rememberExternalDocument(document: Document) {
        fileSystem.absoluteRootPath.resolve(path).bufferedWriter(
            Charset.defaultCharset(),
            32,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { br ->
            document.lines().forEach { br.write(it) }
        }
    }
}

class OsVirtualDirectory(
    override val fsLifetime: Lifetime,
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode {
    internal val mutChildren = ObservableSet(mutableSetOf<OsVFSNode>())
    override val changedEvent = Event<Unit>("OsVirtualDirectory::changedEvent")
    override val children: Set<OsVFSNode> get() = mutChildren

    fun resolveChildNodeOrNull(absolutePath: Path): OsVFSNode? {
        require(absolutePath.isAbsolute) { "Path needs to be absolute!" }
        return children.firstOrNull { absolutePath.startsWith(it.path) }
    }

    fun tryAddChildFiring(toAdd: OsVFSNode): Boolean {
        if (mutChildren.contains(toAdd)) return false
        mutChildren.addFiring(toAdd)
        return true
    }

    fun removeChildFiring(toRemove: OsVFSNode): Boolean {
        if (!mutChildren.contains(toRemove)) return false
        mutChildren.removeFiring(toRemove)
        return true
    }
}

class OsVirtualSymlink(
    override val fsLifetime: Lifetime,
    override val parent: OsVFSNode?,
    override val path: Path,
    override val fileSystem: OsVirtualFileSystem
) : OsVFSNode {
    override val changedEvent = Event<Unit>("OsVirtualSymlink::changedEvent")
    override val children get() = setEmpty
}
