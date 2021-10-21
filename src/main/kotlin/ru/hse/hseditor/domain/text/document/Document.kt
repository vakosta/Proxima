package ru.hse.hseditor.domain.text.document

import ru.hse.hseditor.domain.text.PieceTree


interface Document {
    fun fetchLinesSequence(range: IntRange): Sequence<String>


    val isSyncedWithDisc: Boolean
    val lineCount: Int
}

class PieceTreeDocument(
    val text: PieceTree
) : Document {

    // Add in a text buffer.

    override val isSyncedWithDisc get() = myIsSyncedWithDisc
    private var myIsSyncedWithDisc = true

    override val lineCount get() = text.lineCount

    override fun fetchLinesSequence(range: IntRange) = sequence {
        range.forEach { yield(text.getLineContent(it)) }
    }

    fun fetchLines(range: IntRange) {
    }
}