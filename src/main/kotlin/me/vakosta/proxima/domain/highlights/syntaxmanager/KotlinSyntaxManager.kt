package me.vakosta.proxima.domain.highlights.syntaxmanager

import me.annenkov.proxima.antlr.KotlinLexer
import me.annenkov.proxima.antlr.KotlinParser
import me.vakosta.proxima.data.HighlightInterval
import me.vakosta.proxima.domain.highlights.SyntaxErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import java.util.logging.Logger

class KotlinSyntaxManager(
    val addInterval: (interval: HighlightInterval) -> Unit,
) : SyntaxManager() {

    override fun getTokens(text: String): List<Token> {
        val lexer = KotlinLexer(CharStreams.fromString(text))
        val parser = KotlinParser(CommonTokenStream(lexer))
        parser.addErrorListener(SyntaxErrorListener(this@KotlinSyntaxManager::onError))
        // TODO @thisisvolatile this should support interrupts, meaning that
        // TODO it should have a listener that checks whether we got interrupted or not
        // TODO every time we enter a new node.
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
