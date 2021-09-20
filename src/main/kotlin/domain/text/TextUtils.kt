package domain.text

internal data class Range(
    val startLineNo: Int,
    val startColNo: Int,
    val endLineNo: Int,
    val endColNo: Int,
)