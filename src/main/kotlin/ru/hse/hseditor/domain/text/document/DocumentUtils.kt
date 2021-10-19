package ru.hse.hseditor.domain.text.document

interface DocumentSource {
    fun makeDocument(): Document
    fun commitDocument(document: Document)
}

data class EditorRange(
    val startLineNo: Int,
    val startColNo: Int,
    val endLineNo: Int,
    val endColNo: Int,
)