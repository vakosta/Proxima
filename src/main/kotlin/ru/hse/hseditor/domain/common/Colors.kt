package ru.hse.hseditor.domain.common

import java.awt.Color

val COLOR_BLACK = Color.decode("#000000").rgb
val COLOR_GRAY = Color.decode("#c0c0c0").rgb

val COLOR_SPECIAL = Color.decode("#0033b3").rgb
val COLOR_NUMBER = Color.decode("#1d55ec").rgb
val COLOR_STRING = Color.decode("#007719").rgb

val TOKEN_COLORS = hashMapOf(
    59 to COLOR_SPECIAL,  // CLASS
    61 to COLOR_SPECIAL,  // FUN
    63 to COLOR_SPECIAL,  // VAL
    64 to COLOR_SPECIAL,  // VAR
    75 to COLOR_SPECIAL,  // ELSE
    76 to COLOR_SPECIAL,  // WHEN
    84 to COLOR_SPECIAL,  // RETURN
    124 to COLOR_SPECIAL, // LATEINIT

    133 to COLOR_NUMBER,  // FLOAT
    132 to COLOR_NUMBER,  // DOUBLE
    134 to COLOR_NUMBER,  // LONG
    135 to COLOR_NUMBER,  // INTEGER

    129 to COLOR_STRING,  // QUOTE_OPEN
    155 to COLOR_STRING,  // QUOTE_CLOSE
    157 to COLOR_STRING,  // TEXT
)
