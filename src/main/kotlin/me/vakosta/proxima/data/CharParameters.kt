package me.vakosta.proxima.data

data class CharParameters(
    val color: Int? = null,
    val styles: MutableList<Style> = mutableListOf(),
) {

    enum class Style {
        SELECTED,

        BOLD,
        ITALIC,

        UNDERLINE,
        ERROR,
    }
}
