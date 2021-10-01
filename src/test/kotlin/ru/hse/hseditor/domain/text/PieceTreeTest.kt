package ru.hse.hseditor.domain.text

import kotlin.test.Test
import kotlin.test.assertEquals

class PieceTreeTest {

    private fun PieceTree.addCharByCharAndAssertWithPrefix(str: String, atOffset: Int, prefix: String = "") {
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
}
