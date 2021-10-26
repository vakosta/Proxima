package ru.hse.hseditor.domain.common

import ru.hse.hseditor.antlr.KotlinLexer
import java.awt.Color

val COLOR_BLACK = Color.decode("#000000").rgb

val COLOR_SPECIAL = Color.decode("#0033b3").rgb
val COLOR_NUMBER = Color.decode("#1d55ec").rgb
val COLOR_STRING = Color.decode("#007719").rgb
val COLOR_COMMENT = Color.decode("#afbdb2").rgb

val TOKEN_COLORS = hashMapOf(
    KotlinLexer.CLASS to COLOR_SPECIAL,  // CLASS
    KotlinLexer.FUN to COLOR_SPECIAL,  // FUN
    KotlinLexer.VAL to COLOR_SPECIAL,  // VAL
    KotlinLexer.VAR to COLOR_SPECIAL,  // VAR
    KotlinLexer.ELSE to COLOR_SPECIAL,  // ELSE
    KotlinLexer.WHEN to COLOR_SPECIAL,  // WHEN
    KotlinLexer.RETURN to COLOR_SPECIAL,  // RETURN
    KotlinLexer.LATEINIT to COLOR_SPECIAL, // LATEINIT
    KotlinLexer.IMPORT to COLOR_SPECIAL,
    KotlinLexer.INTERFACE to COLOR_SPECIAL,
    KotlinLexer.OBJECT to COLOR_SPECIAL,
    KotlinLexer.PACKAGE to COLOR_SPECIAL,

    KotlinLexer.FloatLiteral to COLOR_NUMBER,  // FLOAT
    KotlinLexer.DoubleLiteral to COLOR_NUMBER,  // DOUBLE
    KotlinLexer.LongLiteral to COLOR_NUMBER,  // LONG
    KotlinLexer.IntegerLiteral to COLOR_NUMBER,  // INTEGER

    KotlinLexer.QUOTE_OPEN to COLOR_STRING,  // QUOTE_OPEN
    KotlinLexer.QUOTE_CLOSE to COLOR_STRING,  // QUOTE_CLOSE
    KotlinLexer.LineStrText to COLOR_STRING,  // TEXT

    KotlinLexer.DelimitedComment to COLOR_COMMENT,
    KotlinLexer.LineComment to COLOR_COMMENT,
)
