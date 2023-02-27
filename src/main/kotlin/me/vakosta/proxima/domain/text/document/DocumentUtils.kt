package me.vakosta.proxima.domain.text.document

data class EditorRange(
    val startLineNo: Int,
    val startColNo: Int,
    val endLineNo: Int,
    val endColNo: Int,
)
