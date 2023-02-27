package me.vakosta.proxima.domain.highlights.syntaxmanager

import org.antlr.v4.runtime.Token

abstract class SyntaxManager {
    abstract fun getTokens(text: String): List<Token>
}
