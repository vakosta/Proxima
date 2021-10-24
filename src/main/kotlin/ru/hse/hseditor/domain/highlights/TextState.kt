package ru.hse.hseditor.domain.highlights

import com.lodborg.intervaltree.IntegerInterval
import com.lodborg.intervaltree.Interval
import com.lodborg.intervaltree.IntervalTree
import ru.hse.hseditor.data.CharParameters
import ru.hse.hseditor.data.HighlightInterval
import ru.hse.hseditor.domain.common.COLOR_BLACK
import ru.hse.hseditor.domain.common.TOKEN_COLORS
import ru.hse.hseditor.domain.highlights.syntaxmanager.KotlinSyntaxManager
import ru.hse.hseditor.domain.highlights.syntaxmanager.SyntaxManager
import ru.hse.hseditor.domain.text.PieceTree
import ru.hse.hseditor.domain.text.PieceTreeBuilder
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class TextState(
    val text: String,
    val language: Language,
    val pieceTree: PieceTree = PieceTreeBuilder().build(),
    val highlights: IntervalTree<Int> = IntervalTree<Int>(),
    var firstSelectionPosition: Int? = null,
    var secondSelectionPosition: Int? = null,
) {

    private val currentLine: String
        get() {
            val output = pieceTree.getLineContent(caretLine + 1)
            return try {
                output.substring(0, output.indexOfFirst { it == '\n' })
            } catch (e: StringIndexOutOfBoundsException) {
                output
            }
        }

    var caretAbsoluteOffset = 0
        private set
    var caretLine = 0
        private set
    var caretLineOffset = 0
        private set
    val lineStartPosition: Int
        get() = caretAbsoluteOffset - caretLineOffset
    val lineEndPosition: Int
        get() = caretAbsoluteOffset + (currentLine.length - caretLineOffset) - 1

    private val syntaxManager: SyntaxManager
        get() = when (language) {
            Language.Kotlin ->
                KotlinSyntaxManager {}
            Language.Java ->
                KotlinSyntaxManager {}
        }

    fun setCaretAbsoluteOffset(absoluteOffset: Int) {
        // FIXME: Painful loop
        if (absoluteOffset - caretAbsoluteOffset > 0) {
            for (i in 0 until (absoluteOffset - caretAbsoluteOffset)) {
                onPressedRightArrow()
            }
        } else {
            for (i in 0 until (caretAbsoluteOffset - absoluteOffset)) {
                onPressedLeftArrow()
            }
        }
    }

    fun onPressedUpArrow() {
        if (caretLine != 0) {
            caretLine--
            val excessCharNumber = max(0, currentLine.length - caretLineOffset)
            caretAbsoluteOffset -= caretLineOffset + excessCharNumber + 1
            caretLineOffset = min(caretLineOffset, currentLine.length)
        } else {
            caretAbsoluteOffset = 0
            caretLineOffset = 0
        }
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedDownArrow() {
        if (caretLine != pieceTree.lineCount - 1) {
            val currentLineOffset = currentLine.length - caretLineOffset
            caretLine++
            val nextLineOffset = min(caretLineOffset, currentLine.length)
            val excessCharNumber = max(0, currentLineOffset + nextLineOffset)
            caretAbsoluteOffset += excessCharNumber + 1
            caretLineOffset = min(caretLineOffset, currentLine.length)
        } else {
            caretAbsoluteOffset = pieceTree.textLength
            caretLineOffset = currentLine.length
        }
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedLeftArrow() {
        caretAbsoluteOffset = max(caretAbsoluteOffset - 1, 0)
        if (caretLineOffset == 0 && caretLine > 0) {
            caretLine--
            caretLineOffset = currentLine.length
        } else if (caretLineOffset != 0) {
            caretLineOffset--
        }
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedRightArrow() {
        caretAbsoluteOffset = min(caretAbsoluteOffset + 1, pieceTree.textLength)
        if (caretLineOffset == currentLine.length && caretLine < pieceTree.lineCount - 1) {
            caretLine++
            caretLineOffset = 0
        } else if (caretLineOffset != currentLine.length) {
            caretLineOffset++
        }
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedBackspace() {
        pieceTree.deleteAfter(caretAbsoluteOffset - 1)
        updateCurrentLineHighlights(-1)
        onPressedLeftArrow()
    }

    fun onTypedChar(char: Char) {
        pieceTree.insert(char.toString(), caretAbsoluteOffset, true)
        updateCurrentLineHighlights(1)
        onPressedRightArrow()
    }

    fun onAddText(text: String) {
        pieceTree.insert(text, caretAbsoluteOffset, true)
        updateCurrentLineHighlights(text.length)
        for (i in text.indices) { // FIXME: Slow iterator
            onPressedRightArrow()
        }
    }

    private fun updateCurrentLineHighlights(offset: Int) {
        removeIntersectingHighlights(lineStartPosition, lineEndPosition)
        moveHighlights(caretAbsoluteOffset, offset)
        fillHighlights(currentLine, lineStartPosition)
    }

    private fun removeIntersectingHighlights(startPosition: Int, endPosition: Int) {
        val removeInterval = IntegerInterval(
            startPosition,
            endPosition,
            Interval.Bounded.CLOSED
        )
        highlights.query(removeInterval).forEach { highlights.remove(it) }
    }

    private fun moveHighlights(fromPosition: Int, offset: Int) {
        val moveInterval = IntegerInterval(
            fromPosition,
            Interval.Unbounded.CLOSED_LEFT,
        )
        val intervals = highlights.query(moveInterval)
        for (interval in intervals) {
            (interval as HighlightInterval).start += offset
            (interval as HighlightInterval).end += offset
        }
    }

    private fun fillHighlights(text: String, offset: Int = 0) {
        val tokens = syntaxManager.getTokens(text)
        for (token in tokens) {
            val highlightInterval = HighlightInterval(
                start = token.startIndex + offset,
                end = token.stopIndex + offset,
                params = getTokenParams(token.type),
            )
            highlights.add(highlightInterval)
        }
    }

    fun getCharColor(position: Int): Int {
        val color: Int = getHighlights(position).firstOrNull { it.params.color != null }?.params?.color ?: COLOR_BLACK
        return color
    }

    private fun getHighlights(position: Int): Set<HighlightInterval> =
        highlights.query(position) as Set<HighlightInterval>

    private fun getTokenParams(tokenType: Int): CharParameters =
        CharParameters(
            color = getTokenColor(tokenType),
        )

    private fun getTokenColor(tokenType: Int): Int =
        TOKEN_COLORS.getOrDefault(tokenType, COLOR_BLACK)

    enum class Language {
        Kotlin,
        Java,
    }

    companion object {
        val LOG = Logger.getLogger(TextState::class.java.name)
    }
}
