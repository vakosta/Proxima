package ru.hse.hseditor.domain.text.document

abstract class DocumentSource {
    abstract fun makeDocument(): Document
    abstract fun commitDocument()
    abstract fun refreshDocument()

    protected lateinit var myDocument: Document
}

data class EditorRange(
    val startLineNo: Int,
    val startColNo: Int,
    val endLineNo: Int,
    val endColNo: Int,
)