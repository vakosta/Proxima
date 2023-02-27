package me.vakosta.proxima.domain.text.file

// This will get scrapped and rewritten when VFS comes around.
// !!!TEMP!!!

/**
 * Get the list of line start indices starting at 0 (string start)
 */
fun String.getLineStartOffsetsList(): MutableList<Int> {
    val result = mutableListOf(0)

    return fillLineStarts(this, result)
}

private fun fillLineStarts(str: String, result: MutableList<Int>): MutableList<Int> {
    var i = 0
    while (i < str.length) {
        when (str[i]) {
            '\r' -> if (i + 1 < str.length && str[i + 1] == '\n') {
                // got \r\n
                result.add(i + 2)
                ++i
            }
            else {
                // got \r
                result.add(i + 1)
            }
            '\n' -> result.add(i + 1)
        }

        ++i
    }
    return result
}
