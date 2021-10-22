package ru.hse.hseditor.domain.highlights.syntaxmanager

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import ru.hse.hseditor.antlr.KotlinLexer
import ru.hse.hseditor.antlr.KotlinParser
import ru.hse.hseditor.data.HighlightInterval
import ru.hse.hseditor.domain.highlights.SyntaxErrorListener
import java.util.logging.Logger

class KotlinSyntaxManager(
    val addInterval: (interval: HighlightInterval) -> Unit,
) : SyntaxManager() {

    override fun getTokens(text: String): List<Token> {
        val lexer = KotlinLexer(CharStreams.fromString(text))
        val parser = KotlinParser(CommonTokenStream(lexer))
        parser.addErrorListener(SyntaxErrorListener(this@KotlinSyntaxManager::onError))
        parser.statements()
        lexer.reset()
        return lexer.allTokens
    }

    private fun onError(line: Int, charPositionInLine: Int) {
        // addInterval(HighlightInterval())
    }

    companion object {
        val LOG = Logger.getLogger(KotlinSyntaxManager::class.java.name)
    }
}
