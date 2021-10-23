package ru.hse.hseditor.domain.common

import com.lodborg.intervaltree.IntervalTree
import ru.hse.hseditor.data.HighlightInterval

fun IntervalTree<Long>.insertPoint(position: Long) {
    val intervals: MutableSet<HighlightInterval> = this.query(position) as MutableSet<HighlightInterval>
    for (interval in intervals) {
        if (position <= interval.start) {
            interval.start++
            interval.end++
        } else if (position <= interval.end) {
            interval.end++
        }
    }
}
