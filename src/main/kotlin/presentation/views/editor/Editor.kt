package presentation.views.editor

import androidx.compose.runtime.State
import presentation.util.SingleSelection
import java.io.File

class Editor(
    val file: File,
) {
    var close: (() -> Unit)? = null
    lateinit var selection: SingleSelection

    val isActive: Boolean
        get() = selection.selected === this

    fun activate() {
        selection.selected = this
    }

    class Line(val number: Int, val content: Content)

    interface Lines {
        val lineNumberDigitCount: Int get() = size.toString().length
        val size: Int
        operator fun get(index: Int): Line
    }

    class Content(val value: State<String>, val isCode: Boolean)

    fun getLines(): Lines? {
        return object : Lines {
            override val size: Int
                get() = TODO("Not yet implemented")

            override fun get(index: Int): Line {
                TODO("Not yet implemented")
            }
        }// TODO: Get lines
    }
}
