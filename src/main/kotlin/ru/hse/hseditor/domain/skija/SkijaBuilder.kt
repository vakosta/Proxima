package ru.hse.hseditor.domain.skija

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Font
import org.jetbrains.skija.Paint
import org.jetbrains.skija.Rect
import org.jetbrains.skija.Surface
import org.jetbrains.skija.TextLine
import org.jetbrains.skija.Typeface
import org.koin.core.component.KoinComponent
import ru.hse.hseditor.domain.common.COLOR_BLACK
import ru.hse.hseditor.domain.common.COLOR_GRAY
import ru.hse.hseditor.domain.highlights.TextState
import kotlin.math.max
import kotlin.math.min

class SkijaBuilder(
    private val content: String = "",
    private val carriagePosition: Int = 0,
    private val isShowCarriage: Boolean = true,
    private val verticalScrollOffset: Float = 0F,
    private val horizontalScrollOffset: Float = 0F,
    private val width: Int = 1,
    private val height: Int = 1,
    private val textState: TextState? = null,
) : KoinComponent {

    private val surface: Surface = Surface.makeRasterN32Premul(width, height)
    private val paint: Paint = Paint()

    private val baseX
        get() = 5F - horizontalScrollOffset
    private val baseY
        get() = 23F - verticalScrollOffset

    private var x = baseX
    private var y = baseY

    var maxTextX = baseX
        private set
    var maxTextY = baseY
        private set

    fun build(): ImageBitmap {
        paintOnCanvas(surface.canvas, paint)
        return surface.makeImageSnapshot().asImageBitmap()
    }

    private fun paintOnCanvas(
        canvas: Canvas,
        paint: Paint,
    ) {
        for (i in content.indices) {
            val c: Char = content[i]
            if (carriagePosition == i && isShowCarriage) {
                drawCarriage(canvas, paint)
            }
            if (c != '\n') {
                addChar(c, i, canvas, paint)
            } else {
                addNewLine()
            }
        }
        if (carriagePosition == content.length && isShowCarriage) {
            drawCarriage(canvas, paint)
        }
        drawScrollbar(canvas, paint)
    }

    private fun addNewLine() {
        x = baseX
        y += (-font.metrics.ascent + font.metrics.descent + font.metrics.leading).toInt()
        maxTextX = max(x + horizontalScrollOffset, maxTextX)
        maxTextY = max(y + verticalScrollOffset, maxTextY)
    }

    private fun addChar(
        c: Char,
        charPosition: Int,
        canvas: Canvas,
        paint: Paint,
    ) {
        paint.color = textState?.getCharColor(charPosition) ?: COLOR_BLACK
        val textLine = TextLine.make(c.toString(), font)
        canvas.drawTextLine(textLine, x, y, paint)
        x += (textLine.width + 1).toInt()
        maxTextX = max(x + horizontalScrollOffset, maxTextX)
    }

    private fun drawCarriage(
        canvas: Canvas,
        paint: Paint,
    ) {
        paint.color = COLOR_BLACK
        canvas.drawLine(x, y + font.metrics.ascent, x, y + font.metrics.descent, paint)
    }

    private fun drawScrollbar(
        canvas: Canvas,
        paint: Paint,
    ) {
        paint.color = COLOR_GRAY
        val scrollbarHeight = min(height.toFloat(), height.toFloat() / maxTextY * height)
        if (scrollbarHeight == height.toFloat()) {
            return
        }
        val maxOffset = max(1F, maxTextY - height)
        val scrollbarOffset = if (verticalScrollOffset != 0F) verticalScrollOffset / maxOffset else 0F
        val rect = Rect(
            width - 6F,
            (height - scrollbarHeight) * scrollbarOffset + 10,
            width - 18F,
            scrollbarHeight + (height - scrollbarHeight) * scrollbarOffset - 20,
        )
        canvas.drawRect(rect, paint)
    }

    companion object {
        private const val TEXT_SIZE = 14F

        private const val FONT_PATH = "./src/main/resources/fonts/jetbrainsmono/JetBrainsMono-Regular.ttf"

        private var _font: Font? = null
        private val font: Font
            get() {
                if (_font == null) {
                    val typeface = Typeface.makeFromFile(FONT_PATH)
                    _font = Font(typeface, TEXT_SIZE)
                }
                return _font!!
            }
    }
}
