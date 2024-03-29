package me.vakosta.proxima.presentation.model

interface TextLines {
    val size: Int
    fun get(index: Int): String
}

object EmptyTextLines : TextLines {
    override val size: Int
        get() = 0

    override fun get(index: Int): String = ""
}

object BaseTextLines : TextLines {
    override val size: Int
        get() = 12

    override fun get(index: Int): String = "KekCheburek"
}
