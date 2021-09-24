package domain.skija

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Font
import org.jetbrains.skija.Paint
import org.jetbrains.skija.Surface
import org.jetbrains.skija.TextLine
import org.jetbrains.skija.Typeface

class SkijaBuilder(
    private val content: String,
    private val carriagePosition: Int,
    width: Int,
    height: Int,
) {

    private val surface: Surface = Surface.makeRasterN32Premul(width, height)
    private val paint = Paint()

    private var x = BASE_X
    private var y = BASE_Y

    init {
        paint.color = -0xFFFFFF
    }

    fun buildView(): ImageBitmap {
        paintOnCanvas(surface.canvas, paint)
        return surface.makeImageSnapshot().asImageBitmap()
    }

    private fun paintOnCanvas(
        canvas: Canvas,
        paint: Paint,
    ) {
        for (i in content.indices) {
            val c: Char = content[i]
            if (carriagePosition == i) {
                drawCarriage(canvas, paint)
            }
            if (c != '\n') {
                addChar(c, canvas, paint)
            } else {
                addNewLine()
            }
        }
        if (carriagePosition == content.length) {
            drawCarriage(canvas, paint)
        }
    }

    private fun addNewLine() {
        y += (-font.metrics.ascent + font.metrics.descent + font.metrics.leading).toInt()
        x = BASE_X
    }

    private fun addChar(
        c: Char,
        canvas: Canvas,
        paint: Paint
    ) {
        val textLine = TextLine.make(c.toString(), font)
        canvas.drawTextLine(textLine, x, y, paint)
        x += (textLine.width + 1).toInt()
    }

    private fun drawCarriage(
        canvas: Canvas,
        paint: Paint,
    ) {
        canvas.drawLine(x, y + font.metrics.ascent, x, y + font.metrics.descent, paint)
    }

    companion object {
        private const val BASE_X = 5F
        private const val BASE_Y = 23F

        private const val FONT_PATH = "./src/main/resources/fonts/jetbrainsmono/JetBrainsMono-Regular.ttf"

        private var _font: Font? = null
        private val font: Font
            get() {
                if (_font == null) {
                    val typeface = Typeface.makeFromFile(FONT_PATH)
                    _font = Font(typeface, 14F)
                }
                return _font!!
            }
    }
}
