package me.vakosta.proxima.data

import com.lodborg.intervaltree.Interval
import org.antlr.v4.runtime.ParserRuleContext

class HighlightInterval(
    var start: Int,
    var end: Int,
    val params: CharParameters,
    val astNode: ParserRuleContext? = null,
) : Interval<Int>(start, end, Bounded.CLOSED), Comparable<HighlightInterval> {

    override fun create(): Interval<Int> =
            HighlightInterval(start, end, params, astNode)

    override fun getMidpoint(): Int =
        start + (end - start) / 2

    override fun compareTo(other: HighlightInterval): Int =
        midpoint - other.midpoint
}
