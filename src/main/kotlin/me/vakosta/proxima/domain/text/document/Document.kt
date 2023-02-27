package me.vakosta.proxima.domain.text.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.vakosta.proxima.domain.highlights.TextState
import me.vakosta.proxima.domain.text.PieceTree

abstract class DocumentSource {
    abstract suspend fun makeDocument(): Document?
    abstract suspend fun rememberOpenedDocument(document: Document)
    abstract suspend fun refreshOpenedDocument(document: Document)

    abstract suspend fun rememberExternalDocument(document: Document)

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
    val source: DocumentSource?

    suspend fun writeToMainSource() {
        source?.rememberOpenedDocument(this)
    }

    suspend fun updateFromSource() {
        source?.refreshOpenedDocument(this)
    }

    suspend fun writeToSecondarySource(source: DocumentSource) {
        source.rememberExternalDocument(this)
    }
}

class PieceTreeDocument(
    override val source: DocumentSource?,
    private val myPieceTree: PieceTree
) : Document {
    override var isSyncedWithDisc: Boolean by mutableStateOf(true)

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
