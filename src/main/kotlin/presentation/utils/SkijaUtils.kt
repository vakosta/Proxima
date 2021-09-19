package presentation.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Font
import org.jetbrains.skija.FontStyle
import org.jetbrains.skija.Paint
import org.jetbrains.skija.Surface
import org.jetbrains.skija.TextLine
import org.jetbrains.skija.Typeface

fun String.codeView(
    width: Int,
    height: Int,
): ImageBitmap {
    val surface = Surface.makeRasterN32Premul(width, height)
    val paint = Paint()
    paint.color = -0xFFFFFF
    val font = getFont()
    buildBitmap(surface.canvas, paint, font)
    return surface.makeImageSnapshot().asImageBitmap()
}

private fun getFont(): Font {
    val typeface = Typeface.makeFromName("Consolas", FontStyle.NORMAL)
    return Font(typeface, 16F)
}

private fun String.buildBitmap(
    canvas: Canvas,
    paint: Paint,
    font: Font,
) {
    var x = 3
    var y = 15
    for (i in this.indices) {
        val c: Char = this[i]
        if (c != '\n') {
            val textLine = TextLine.make(c.toString(), font)
            canvas.drawTextLine(textLine, x.toFloat(), y.toFloat(), paint)
            x += (textLine.width + 1).toInt()
        } else {
            y += (-font.metrics.ascent + font.metrics.descent + font.metrics.leading).toInt()
            x = 3
        }
    }
    canvas.drawLine(
        x.toFloat(),
        y.toFloat() - font.metrics.height,
        x.toFloat(),
        y.toFloat() + font.metrics.descent,
        paint
    )
}
