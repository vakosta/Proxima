package ru.hse.hseditor.domain.highlights

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class SyntaxErrorListener(
    private val onError: (line: Int, charPositionInLine: Int) -> Unit,
) : BaseErrorListener() {

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?,
    ) = onError(line, charPositionInLine)
}
