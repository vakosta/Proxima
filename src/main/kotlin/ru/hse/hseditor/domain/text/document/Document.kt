package ru.hse.hseditor.domain.text.document

import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.vfs.NodeChangeKind
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.domain.text.PieceTree

abstract class DocumentSource {
    abstract suspend fun makeDocumentSuspend(): Document?
    abstract fun commitDocument(document: Document)
    abstract fun refreshDocument(document: Document)

    val openedDocuments = mutableListOf<Document>()
}

interface Document {
    fun lines(range: IntRange): Sequence<String>
    fun lines(): Sequence<String>

    fun getRawContent(): String

    var isSyncedWithDisc: Boolean

    val lineCount: Int
    val textLength: Int

    fun getLineContent(lineNo: Int): String
    fun deleteCharAfter(offset: Int, cnt: Int = 1)
    fun insert(str: String, atOffset: Int)

    var textState: TextState
    val source: DocumentSource
}

fun Document.handleChangesFromSource(lifetime: Lifetime, kind: NodeChangeKind) {
    if (isSyncedWithDisc) {
        textState
    } else {

    }
}

class PieceTreeDocument(
    override val source: DocumentSource,
    private val myPieceTree: PieceTree
) : Document {
    override var isSyncedWithDisc = true

    override val lineCount get() = myPieceTree.lineCount
    override val textLength get() = myPieceTree.textLength

    override lateinit var textState: TextState

    override fun getLineContent(lineNo: Int) = myPieceTree.getLineContent(lineNo)

    override fun deleteCharAfter(offset: Int, cnt: Int) {
        myPieceTree.deleteAfter(offset, cnt)
        isSyncedWithDisc = false
    }

    override fun insert(str: String, atOffset: Int) {
        myPieceTree.insert(str, atOffset)
        isSyncedWithDisc = false
    }

    override fun lines(range: IntRange) = sequence {
        range.forEach { yield(myPieceTree.getLineContent(it)) }
    }

    override fun lines() = lines(1..lineCount)

    override fun getRawContent() = myPieceTree.getLinesRawContent()
}