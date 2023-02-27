package me.vakosta.proxima.domain.common

import com.lodborg.intervaltree.IntervalTree
import me.vakosta.proxima.data.HighlightInterval

fun IntervalTree<Long>.insertPoint(position: Long) {
    val intervals: MutableSet<me.vakosta.proxima.data.HighlightInterval> = this.query(position) as MutableSet<me.vakosta.proxima.data.HighlightInterval>
    for (interval in intervals) {
        if (position <= interval.start) {
            interval.start++
            interval.end++
        } else if (position <= interval.end) {
            interval.end++
        }
    }
}
