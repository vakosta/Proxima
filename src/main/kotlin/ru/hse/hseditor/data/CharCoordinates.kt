package ru.hse.hseditor.data

data class CharCoordinates(
    val x: Float,
    val y: Float,
    val absoluteOffset: Int = -1,
) : Comparable<CharCoordinates> {

    private fun compareX(other: Float): Int {
        val diff = x - other
        val minBarrier = 0
        val maxBarrier = 0
        return if (diff < minBarrier) {
            -1
        } else if (diff > maxBarrier) {
            1
        } else {
            0
        }
    }

    private fun compareY(other: Float): Int {
        val diff = y - other
        val minBarrier = 0
        val maxBarrier = 18
        return if (diff < minBarrier) {
            -1
        } else if (diff > maxBarrier) {
            1
        } else {
            0
        }
    }

    override fun compareTo(other: CharCoordinates): Int {
        val xDiff = compareX(other.x)
        val yDiff = compareY(other.y)
        return if (yDiff < 0) {
            -1
        } else if (yDiff > 0) {
            1
        } else if (xDiff < 0) {
            -1
        } else if (xDiff > 0) {
            1
        } else {
            0
        }
    }
}
