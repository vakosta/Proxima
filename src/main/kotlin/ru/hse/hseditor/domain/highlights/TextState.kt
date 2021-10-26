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
import kotlin.math.abs
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

    private val syntaxManager: SyntaxManager
        get() = when (language) {
            Language.Kotlin ->
                KotlinSyntaxManager {}
            Language.Java ->
                KotlinSyntaxManager {}
        }

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

    fun clearSelectionPositions() {
        firstSelectionPosition = null
        secondSelectionPosition = null
    }

    fun setCaretAbsoluteOffset(absoluteOffset: Int, withSelection: Boolean) {
        // FIXME: Painful loop
        val offsetDiff = absoluteOffset - caretAbsoluteOffset
        if (offsetDiff > 0) {
            for (i in 0 until abs(offsetDiff)) {
                onPressedRightArrow(withSelection)
            }
        } else {
            for (i in 0 until abs(offsetDiff)) {
                onPressedLeftArrow(withSelection)
            }
        }
    }

    fun onPressedUpArrow(withSelection: Boolean) {
        if (caretLine != 0) {
            caretLine--
            val excessCharNumber = caretLineOffset + max(0, currentLine.length - caretLineOffset)
            caretAbsoluteOffset -= excessCharNumber + 1
            caretLineOffset = min(caretLineOffset, currentLine.length)
        } else {
            caretAbsoluteOffset = 0
            caretLineOffset = 0
        }
        updateSelection(withSelection)
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedDownArrow(withSelection: Boolean) {
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
        updateSelection(withSelection)
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedLeftArrow(withSelection: Boolean) {
        caretAbsoluteOffset = max(caretAbsoluteOffset - 1, 0)
        if (caretLineOffset == 0 && caretLine > 0) {
            caretLine--
            caretLineOffset = currentLine.length
        } else if (caretLineOffset != 0) {
            caretLineOffset--
        }
        updateSelection(withSelection)
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedRightArrow(withSelection: Boolean) {
        caretAbsoluteOffset = min(caretAbsoluteOffset + 1, pieceTree.textLength)
        if (caretLineOffset == currentLine.length && caretLine < pieceTree.lineCount - 1) {
            caretLine++
            caretLineOffset = 0
        } else if (caretLineOffset != currentLine.length) {
            caretLineOffset++
        }
        updateSelection(withSelection)
        LOG.log(Level.INFO, "Carriage position: $caretLine:$caretLineOffset")
    }

    fun onPressedBackspace() {
        pieceTree.deleteAfter(caretAbsoluteOffset - 1)
        updateCurrentLineHighlights(-1)
        onPressedLeftArrow(false)
    }

    fun onTypedChar(char: Char) {
        pieceTree.insert(char.toString(), caretAbsoluteOffset, true)
        updateCurrentLineHighlights(1)
        onPressedRightArrow(false)
    }

    fun onAddText(text: String) {
        pieceTree.insert(text, caretAbsoluteOffset, true)
        updateCurrentLineHighlights(text.length)
        for (i in text.indices) { // FIXME: Slow iterator
            onPressedRightArrow(false)
        }
    }

    fun getCharColor(position: Int): Int {
        val color: Int = getHighlights(position).firstOrNull { it.params.color != null }?.params?.color ?: COLOR_BLACK
        return color
    }

    private fun updateSelection(withSelection: Boolean) {
        if (withSelection) {
            secondSelectionPosition = caretAbsoluteOffset
        } else {
            clearSelectionPositions()
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
