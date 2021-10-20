package ru.hse.hseditor.domain.lexer

import org.antlr.v4.runtime.Token

interface LexerService {
    val allTokens: List<Token>
}
