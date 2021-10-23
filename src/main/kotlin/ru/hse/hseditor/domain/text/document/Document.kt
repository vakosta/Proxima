package ru.hse.hseditor.domain.text.document

import ru.hse.hseditor.domain.text.PieceTree

abstract class DocumentSource {
    abstract fun makeDocument(): Document
    abstract fun commitDocument()
    abstract fun refreshDocument()

    protected lateinit var myDocument: Document
}

interface Document {
    fun lines(range: IntRange): Sequence<String>
    fun lines(): Sequence<String>

    fun getRawContent(): String

    val isSyncedWithDisc: Boolean

    val lineCount: Int
    val textLength: Int

    fun getLineContent(lineNo: Int): String
    fun deleteCharAfter(offset: Int, cnt: Int = 1)
    fun insert(str: String, atOffset: Int)
}

class PieceTreeDocument(
    private val myPieceTree: PieceTree
) : Document {
    override val isSyncedWithDisc get() = myIsSyncedWithDisc
    private var myIsSyncedWithDisc = true

    override val lineCount get() = myPieceTree.lineCount
    override val textLength get() = myPieceTree.textLength

    override fun getLineContent(lineNo: Int) = myPieceTree.getLineContent(lineNo)

    override fun deleteCharAfter(offset: Int, cnt: Int) = myPieceTree.deleteAfter(offset, cnt)

    override fun insert(str: String, atOffset: Int) = myPieceTree.insert(str, atOffset)

    override fun lines(range: IntRange) = sequence {
        range.forEach { yield(myPieceTree.getLineContent(it)) }
    }

    override fun lines() = lines(1..lineCount)

    override fun getRawContent() = myPieceTree.getLinesRawContent()

    fun fetchLines(range: IntRange) {
    }
}