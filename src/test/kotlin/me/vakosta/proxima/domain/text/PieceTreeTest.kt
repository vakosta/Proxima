package me.vakosta.proxima.domain.text

import kotlin.test.Test
import kotlin.test.assertEquals

class PieceTreeTest {

    private fun PieceTree.addCharByCharAndAssertWithPrefix(str: String, atOffset: Int, prefix: String = this.getLinesRawContent().slice(0 until atOffset)) {
        val resBuilder = StringBuilder().apply { append(prefix) }

        for (i in str.indices) {
            insert(str[i].toString(), atOffset + i)
            resBuilder.append(str[i])
            assertEquals(resBuilder.toString(), getLinesRawContent())
        }
    }

    private fun PieceTree.deleteAllCharByCharAndAssert(fromOffset: Int) {
        val resBuilder = StringBuilder().apply { append(getLinesRawContent()) }
        val str = resBuilder.toString()
        for (i in str.indices.reversed()) {
            deleteAfter(i, 1)
            resBuilder.delete(i, i + 1)
            assertEquals(resBuilder.toString(), getLinesRawContent())
        }
    }

    private fun PieceTree.deleteCharByChar(fromOffset: Int, cnt: Int) {
        val resBuilder = StringBuilder().apply { append(getLinesRawContent()) }
        for (i in (fromOffset - cnt..fromOffset).reversed()) {
            deleteAfter(i, 1)
            resBuilder.delete(i, i + 1)
            val content = getLinesRawContent()
            assertEquals(resBuilder.toString(), content)
        }
    }

    @Test
    fun `basic add`() {
        val pieceTreeBuilder = PieceTreeBuilder()
        pieceTreeBuilder.acceptChunkString("I love cats!\n")
        pieceTreeBuilder.acceptChunkString("I love dogs!")

        val pieceTree = pieceTreeBuilder.build()

        assertEquals(2, pieceTree.lineCount)
        assertEquals("I love cats!\n", pieceTree.getLineContent(1))
        assertEquals("I love dogs!", pieceTree.getLineContent(2))

        assertEquals("I love cats!\nI love dogs!", pieceTree.getLinesRawContent())

        pieceTree.insert("+", 1)
        assertEquals(2, pieceTree.lineCount)
        assertEquals("I+ love cats!\n", pieceTree.getLineContent(1))
        assertEquals("I love dogs!", pieceTree.getLineContent(2))
    }

    @Test
    fun `add chars one by one`() {
        val pieceTree = PieceTreeBuilder().build()
        pieceTree.addCharByCharAndAssertWithPrefix("abacaba", 0)
    }

    @Test
    fun `basic delete`() {
        val pieceTree = PieceTreeBuilder().build()

        pieceTree.insert("abacaba", 0)

        pieceTree.deleteAfter(4, 3)

        assertEquals("abac", pieceTree.getLinesRawContent())
    }

    @Test
    fun `delete chars one by one`() {
        val pieceTree = PieceTreeBuilder().build()

        pieceTree.insert("a", 0)

        pieceTree.deleteAllCharByCharAndAssert(7)
    }

    @Test
    fun `delete from long text`() {
        val pieceTree = PieceTreeBuilder().build()

        val str = "abacabaabacabaabacabaabacabaabacabaabacabaabacabaabacabaabacaba"
        pieceTree.addCharByCharAndAssertWithPrefix(str, 0)
        pieceTree.deleteAllCharByCharAndAssert(str.length)

        assertEquals("", pieceTree.getLinesRawContent())

        pieceTree.addCharByCharAndAssertWithPrefix(str, 0)
        pieceTree.deleteAfter(str.length - 1, 1)

        assertEquals(str.substring(0 until str.lastIndex), pieceTree.getLinesRawContent())
    }

    @Test
    fun `delete after had a linebreak`() {
        val pieceTree = PieceTreeBuilder().build()

        val str = "abacabaabacabaabacabaabacaba\nabacabaabacabaabacabaabacabaabacaba"
        pieceTree.addCharByCharAndAssertWithPrefix(str, 0)
        pieceTree.deleteAllCharByCharAndAssert(str.length)

        assertEquals("", pieceTree.getLinesRawContent())

        val shortStr = "I love cats!"
        pieceTree.addCharByCharAndAssertWithPrefix(shortStr, 0)
        pieceTree.deleteAfter(shortStr.length - 1, 1)

        assertEquals(shortStr.substring(0 until shortStr.lastIndex), pieceTree.getLinesRawContent())
    }

    @Test
    fun `delete from the middle`() {
        val pieceTree = PieceTreeBuilder().build()

        val str = "abc"
        pieceTree.addCharByCharAndAssertWithPrefix(str, 0)
        pieceTree.deleteAfter(str.length - 2, 1)

        assertEquals("ac", pieceTree.getLinesRawContent())
    }

    @Test
    fun `the kek example (insert 2 middle) works`() {
        val pieceTree = PieceTreeBuilder().build()

        pieceTree.addCharByCharAndAssertWithPrefix("val kek = 123", 0)
        pieceTree.deleteAfter(6)
        assertEquals("val ke = 123", pieceTree.getLinesRawContent())
        pieceTree.insert("k", 6)
        assertEquals("val kek = 123", pieceTree.getLinesRawContent())
        pieceTree.deleteAfter(6)
        assertEquals("val ke = 123", pieceTree.getLinesRawContent())
    }

    @Test
    fun `the kot example (newlines) works`() {
        val pieceTree = PieceTreeBuilder().build()

        val str = "val kot = 123\nval kot = 123\nval kot = 123\nval kot = 123\n"
        pieceTree.addCharByCharAndAssertWithPrefix(str, 0)
        assertEquals(str, pieceTree.getLinesRawContent())
    }

    @Test
    fun `newlines end lines`() {
        val pieceTree = PieceTreeBuilder().build()

        val line = "val kot = 123\n"
        val str = "val kot = 123\nval kot = 123\nval kot = 123\nval kot = 123\n"
        pieceTree.addCharByCharAndAssertWithPrefix(str, 0)
        for (i in 1..4) {
            assertEquals(line, pieceTree.getLineContent(i))
        }
    }

    @Test
    fun `stdio bug`() {
        val pieceTree = PieceTreeBuilder().build()

        pieceTree.addCharByCharAndAssertWithPrefix("#include <>", 0)

        var offset = 10
        for (ch in "std") {
            pieceTree.insert(ch.toString(), offset)
            offset += 1
        }
        println(pieceTree.getLinesRawContent())
        pieceTree.deleteBefore(offset)
        offset -= 1

        val sb = StringBuilder().apply { append(pieceTree.getLinesRawContent()) }
        for (ch in "dio.h") {
            pieceTree.insert(ch.toString(), offset)
            sb.insert(offset, ch)
            assertEquals(sb.toString(), pieceTree.getLinesRawContent())
            offset += 1
        }

        println(pieceTree.getLinesRawContent())
    }
}
