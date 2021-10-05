package ru.hse.hseditor.data

import com.lodborg.intervaltree.Interval

class HighlightInterval(
    var start: Int,
    var end: Int,
    val params: CharParameters,
) : Interval<Int>(start, end, Bounded.CLOSED), Comparable<HighlightInterval> {

    override fun create(): Interval<Int> =
        HighlightInterval(start, end, params)

    override fun getMidpoint(): Int =
        start + (end - start) / 2

    override fun compareTo(other: HighlightInterval): Int =
        midpoint - other.midpoint
}
