package ru.hse.hseditor.domain.text.document

import ru.hse.hseditor.domain.text.PieceTree


interface Document {

}

class PieceTreeDocument(
    val text: PieceTree
) : Document {

    // Add in a text buffer.

    var isSyncedWithDisc = true

}