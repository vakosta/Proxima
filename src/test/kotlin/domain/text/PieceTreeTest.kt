package domain.text

import kotlin.test.*

class PieceTreeTest {

    @Test fun `basic add`() {
        val pieceTreeBuilder = PieceTreeBuilder()
        pieceTreeBuilder.acceptChunkString("I love cats!\n")
        pieceTreeBuilder.acceptChunkString("I love dogs!")

        val pieceTree = pieceTreeBuilder.buildPieceTree()

        assertEquals(2, pieceTree.lineCount)
        assertEquals("I love cats!\n", pieceTree.getLineContent(1))
        assertEquals("I love dogs!", pieceTree.getLineContent(2))

        assertEquals("I love cats!\nI love dogs!", pieceTree.getLinesRawContent())

        pieceTree.insert("+", 1, true)
        assertEquals(2, pieceTree.lineCount)
        assertEquals("I+ love cats!\n", pieceTree.getLineContent(1))
        assertEquals("I love dogs!", pieceTree.getLineContent(2))
    }
}