package ru.hse.hseditor.domain.highlights

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lodborg.intervaltree.IntegerInterval
import com.lodborg.intervaltree.Interval
import com.lodborg.intervaltree.IntervalTree
import ru.hse.hseditor.data.CharParameters
import ru.hse.hseditor.data.HighlightInterval
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.COLOR_BLACK
import ru.hse.hseditor.domain.common.Event
import ru.hse.hseditor.domain.common.TOKEN_COLORS
import ru.hse.hseditor.domain.highlights.syntaxmanager.KotlinSyntaxManager
import ru.hse.hseditor.domain.highlights.syntaxmanager.SyntaxManager
import ru.hse.hseditor.domain.text.document.Document
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

enum class ExtModificationKind {
    DOCUMENT_DISC_SYNC,
    /* PSI_MODIFICATION, */
    /* REFORMAT, */
    /* ... etc ... */

    DOCUMENT_DELETED
}

// May have to attach some more data in the future
data class ExtModificationDesc(val kind: ExtModificationKind)

class TextState(
    private val myLifetime: Lifetime,
    val language: Language,
    var document: Document,
    val highlights: IntervalTree<Int> = IntervalTree<Int>(),
) {

    val externalModificationEvent = Event<ExtModificationDesc>("TextState::externalModificationEvent")

    init {
        document.textState = this
        fillHighlights(document.getRawContent())
    }

    private val currentLine: String
        get() {
            val output = document.getLineContent(carriageLine + 1)
            return try {
                output.substring(0, output.indexOfFirst { it == '\n' })
            } catch (e: StringIndexOutOfBoundsException) {
                output
            }
        }

    var carriageAbsoluteOffset = 0
        private set
    var carriageLine = 0
        private set
    var carriageLineOffset = 0
        private set
    val lineStartPosition: Int
        get() = carriageAbsoluteOffset - carriageLineOffset
    val lineEndPosition: Int
        get() = carriageAbsoluteOffset + (currentLine.length - carriageLineOffset) - 1

    private val syntaxManager: SyntaxManager
        get() = when (language) {
            Language.Kotlin ->
                KotlinSyntaxManager {}
            Language.Java ->
                KotlinSyntaxManager {}
        }

    fun onPressedLeftArrow() {
        carriageAbsoluteOffset = max(carriageAbsoluteOffset - 1, 0)
        if (carriageLineOffset == 0 && carriageLine > 0) {
            carriageLine--
            carriageLineOffset = currentLine.length
        } else if (carriageLineOffset != 0) {
            carriageLineOffset--
        }
        LOG.log(Level.INFO, "Carriage position: $carriageLine:$carriageLineOffset")
    }

    fun onPressedRightArrow() {
        carriageAbsoluteOffset = min(carriageAbsoluteOffset + 1, document.textLength)
        if (carriageLineOffset == currentLine.length && carriageLine < document.lineCount - 1) {
            carriageLine++
            carriageLineOffset = 0
        } else if (carriageLineOffset != currentLine.length) {
            carriageLineOffset++
        }
        LOG.log(Level.INFO, "$carriageLine $carriageLineOffset")
    }

    fun onPressedBackspace() {
        document.deleteCharAfter(carriageAbsoluteOffset - 1)
        updateCurrentLineHighlights(-1)
        onPressedLeftArrow()
    }

    fun onTypedChar(char: Char) {
        document.insert(char.toString(), carriageAbsoluteOffset)
        updateCurrentLineHighlights(1)
        onPressedRightArrow()
    }

    private fun updateCurrentLineHighlights(offset: Int) {
        removeIntersectingHighlights(lineStartPosition, lineEndPosition)
        moveHighlights(carriageAbsoluteOffset, offset)
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
        val LOG = Logger.getLogger(TextState::class.java.name).apply { setFilter { false } }
    }
}
