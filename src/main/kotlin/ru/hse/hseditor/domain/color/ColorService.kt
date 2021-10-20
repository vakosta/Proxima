package ru.hse.hseditor.domain.color

import org.antlr.v4.runtime.Token
import ru.hse.hseditor.domain.common.COLOR_BLACK
import ru.hse.hseditor.domain.common.COLOR_NUMBER
import ru.hse.hseditor.domain.common.COLOR_SPECIAL
import ru.hse.hseditor.domain.common.COLOR_STRING
import ru.hse.hseditor.domain.lexer.KotlinLexerService
import ru.hse.hseditor.domain.lexer.LexerService

class ColorService(
    private val content: String,
    private val language: Language,
) {

    private val lexer: LexerService
        get() =
            when (language) {
                Language.Kotlin ->
                    KotlinLexerService(content)
                Language.Java ->
                    KotlinLexerService(content)
            }

    fun getCharColor(
        position: Int,
    ): Int {
        val currentToken: Token? = getToken(position)
        return getTokenColor(currentToken)
    }

    private fun getToken(position: Int): Token? {
        var currentToken: Token? = null
        for (token in lexer.allTokens) {
            if (token.startIndex <= position && token.stopIndex >= position) {
                currentToken = token
                break
            }
        }
        return currentToken
    }

    private fun getTokenColor(currentToken: Token?): Int =
        when (currentToken?.type) {
            59 -> COLOR_SPECIAL  // CLASS
            61 -> COLOR_SPECIAL  // FUN
            63 -> COLOR_SPECIAL  // VAL
            64 -> COLOR_SPECIAL  // VAR
            75 -> COLOR_SPECIAL  // ELSE
            76 -> COLOR_SPECIAL  // WHEN
            84 -> COLOR_SPECIAL  // RETURN
            124 -> COLOR_SPECIAL // LATEINIT

            133 -> COLOR_NUMBER  // FLOAT
            132 -> COLOR_NUMBER  // DOUBLE
            134 -> COLOR_NUMBER  // LONG
            135 -> COLOR_NUMBER  // INTEGER

            129 -> COLOR_STRING  // QUOTE_OPEN
            155 -> COLOR_STRING  // QUOTE_CLOSE
            157 -> COLOR_STRING  // TEXT

            else -> COLOR_BLACK
        }

    enum class Language {
        Kotlin,
        Java,
    }
}
