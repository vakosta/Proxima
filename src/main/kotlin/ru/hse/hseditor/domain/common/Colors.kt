package ru.hse.hseditor.domain.common

import ru.hse.hseditor.antlr.KotlinLexer
import java.awt.Color

val COLOR_BLACK = Color.decode("#000000").rgb
val COLOR_GRAY = Color.decode("#c0c0c0").rgb
val COLOR_BLUE = Color.decode("#a6d2ff").rgb

val COLOR_SPECIAL = Color.decode("#0033b3").rgb
val COLOR_ANNOTATION = Color.decode("#af9a00").rgb
val COLOR_NUMBER = Color.decode("#1d55ec").rgb
val COLOR_STRING = Color.decode("#007719").rgb
val COLOR_COMMENT = Color.decode("#afbdb2").rgb

val TOKEN_COLORS = hashMapOf(
    KotlinLexer.CLASS to COLOR_SPECIAL,
    KotlinLexer.FUN to COLOR_SPECIAL,
    KotlinLexer.VAL to COLOR_SPECIAL,
    KotlinLexer.VAR to COLOR_SPECIAL,
    KotlinLexer.IF to COLOR_SPECIAL,
    KotlinLexer.ELSE to COLOR_SPECIAL,
    KotlinLexer.TRY to COLOR_SPECIAL,
    KotlinLexer.CATCH to COLOR_SPECIAL,
    KotlinLexer.PRIVATE to COLOR_SPECIAL,
    KotlinLexer.PUBLIC to COLOR_SPECIAL,
    KotlinLexer.WHEN to COLOR_SPECIAL,
    KotlinLexer.RETURN to COLOR_SPECIAL,
    KotlinLexer.LATEINIT to COLOR_SPECIAL,
    KotlinLexer.IMPORT to COLOR_SPECIAL,
    KotlinLexer.INTERFACE to COLOR_SPECIAL,
    KotlinLexer.OBJECT to COLOR_SPECIAL,
    KotlinLexer.PACKAGE to COLOR_SPECIAL,
    KotlinLexer.ANNOTATION to COLOR_ANNOTATION,

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
