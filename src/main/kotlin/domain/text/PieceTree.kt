package domain.text

import domain.text.file.DiffChunk
import domain.text.file.FileChunk
import domain.text.file.getLineStartOffsetsList

//region Misc. declarations

internal const val PREFERRED_PIECE_TREE_CHUNK_SIZE = 65535

internal enum class PieceChunkKind { FILE, DIFF }

/**
 * Used in [PieceTreeSearchCache] as a return type.
 */
internal data class NodeLookupResult(val node: PieceTreeNode, val nodeStartOffset: Int, val pieceOffset: Int)

/**
 * Describes a text chunk inside the [PieceTree].
 */
internal data class PieceChunkDescriptor(val bufferKind: PieceChunkKind, val bufferIndex: Int)

/**
 * Is used to facilitate faster text lookup inside a chunk.
 */
internal data class PieceChunkLineColOffset(val lineNo: Int, val colNo: Int)

internal data class Piece(
    val chunkDesc: PieceChunkDescriptor,
    val chunkStartPos: PieceChunkLineColOffset,
    val editorEnd: PieceChunkLineColOffset,
    val lineFeeds: Int,
    val textLen: Int
)

//endregion

//region PieceTree node

internal enum class NodeColor { Black, Red }

internal val SENTINEL = PieceTreeNode(
    NodeColor.Black,
    Piece(
        PieceChunkDescriptor(PieceChunkKind.FILE, -1),
        PieceChunkLineColOffset(-1, -1),
        PieceChunkLineColOffset(-1, -1),
        -1,
        -1
    )
).apply {
    parent = this
    left = this
    right = this
}

internal class PieceTreeNode(
    var color: NodeColor,
    var piece: Piece
) {
    var parent: PieceTreeNode = this
    var left: PieceTreeNode = this
    var right: PieceTreeNode = this

    val grandparent get() = parent.parent

    internal var textLenLeft = 0
    internal var lineFeedsLeft = 0

    val isLeftChild get() = parent.left == this
    val isRightChild get() = parent.right == this

    //region Text-ordered tree traversal

    val leftmostChild: PieceTreeNode
        get() {
            var travRoot = this
            while (travRoot.left != SENTINEL) {
                travRoot = travRoot.left
            }
            return travRoot
        }

    val rigthmostChild: PieceTreeNode
        get() {
            var travRoot = this
            while (travRoot.right != SENTINEL) {
                travRoot = travRoot.right
            }
            return travRoot
        }

    val next: PieceTreeNode
        get() {
            if (right != SENTINEL) return right.leftmostChild

            // Traverse up until a node that is a left child
            var travRoot = this
            while (travRoot.parent != SENTINEL) {
                if (travRoot.isLeftChild) break
                travRoot = travRoot.parent
            }

            return if (travRoot.parent == SENTINEL) SENTINEL else travRoot.parent
        }

    val prev: PieceTreeNode
        get() {
            if (left != SENTINEL) return left.rigthmostChild

            // Traverse up until a node that is a right child
            var travRoot = this
            while (travRoot.parent != SENTINEL) {
                if (travRoot.isRightChild) break
                travRoot = travRoot.parent
            }

            return if (travRoot.parent == SENTINEL) SENTINEL else travRoot.parent
        }

    //endregion

    // The length of the text if this node is the root
    val subtreeTextLen get(): Int = if (this == SENTINEL) 0 else textLenLeft + piece.textLen + right.subtreeTextLen

    val subtreeLineFeedsCount get(): Int = if (this == SENTINEL) 0 else lineFeedsLeft + piece.lineFeeds + right.subtreeLineFeedsCount

    fun detach() {
        parent = SENTINEL
        left = SENTINEL
        right = SENTINEL
    }
}

//endregion

//region Node search cache

internal data class PieceTreeSearchCacheEntry(
    val node: PieceTreeNode,
    val nodeStartOffset: Int,
    val nodeStartLineNumber: Int?
)

internal class PieceTreeSearchCache(
    private val myLimit: Int
) {
    private var myCache = mutableListOf<PieceTreeSearchCacheEntry>()

    fun getEntryByOffset(offset: Int): PieceTreeSearchCacheEntry? {
        for (entry in myCache.reversed()) {
            if (entry.nodeStartOffset <= offset
                && entry.nodeStartOffset + entry.node.piece.textLen >= offset
            ) {
                return entry
            }
        }
        return null
    }


    fun getEntryByLineNumber(lineNumber: Int): PieceTreeSearchCacheEntry? {
        for (entry in myCache.reversed()) {
            if (entry.nodeStartLineNumber != null && entry.nodeStartLineNumber < lineNumber
                && entry.nodeStartLineNumber + entry.node.piece.lineFeeds >= lineNumber
            ) {
                return entry
            }
        }
        return null
    }

    fun addEntry(entry: PieceTreeSearchCacheEntry) {
        if (myCache.size >= myLimit) {
            myCache.removeFirst()
        }
        myCache.add(entry)
    }

    /**
     * Validates the cache to have no "dead" entries at offset.
     */
    fun validate(offset: Int) {
        val tmp = mutableListOf<PieceTreeSearchCacheEntry>()
        for (i in myCache.indices) {
            val entry = myCache[i]
            if (entry.nodeStartOffset < offset) {
                tmp.add(entry)
            }
        }

        myCache = tmp
    }
}

//endregion

//region PieceTree

class PieceTree(
    private val myFileChunks: MutableList<FileChunk>,
    private var myIsEolNormalized: Boolean
) {

    private val myDiffChunks = mutableListOf(DiffChunk(StringBuilder(), mutableListOf()))
    private var myRoot = SENTINEL

    private var myLineFeedsCnt: Int = 1
    val lineCount get() = myLineFeedsCnt

    private var myTextLength: Int = 0

    private var myLastChangedLineNo = 0
    private var myLastChangedColNo = 0

    private var myLastVisitedLineNo = 0
    private var myLastVisitedLineValue = ""

    private val mySearchCache = PieceTreeSearchCache(10)

    init {
        initPiecesFromFileChunks()
        recomputeGlobalTextMetadata()
        myRoot.recomputeSubtreeTextMetadata()
    }

    private fun initPiecesFromFileChunks() {
        var lastNode: PieceTreeNode? = null
        for (i in myFileChunks.indices) {
            if (myFileChunks[i].chunk.isNotEmpty()) {
                if (myFileChunks[i].lineStartOffsets.isEmpty()) {
                    myFileChunks[i].lineStartOffsets = myFileChunks[i].chunk.getLineStartOffsetsList()
                }

                val piece = Piece(
                    PieceChunkDescriptor(PieceChunkKind.FILE, i),
                    PieceChunkLineColOffset(0, 0),
                    PieceChunkLineColOffset(
                        myFileChunks[i].lineStartOffsets.lastIndex,
                        myFileChunks[i].chunk.length - myFileChunks[i].lineStartOffsets.last()
                    ),
                    myFileChunks[i].lineStartOffsets.lastIndex,
                    myFileChunks[i].chunk.length
                )
                lastNode = rbInsertNodeFromPieceRight(lastNode, piece)
            }
        }
    }

    fun getContentInEditorRange(range: EditorRange): String {
        if (range.startLineNo == range.endLineNo && range.startColNo == range.endColNo) {
            return ""
        }

        val startLookup = lookupNodeAtEditorPos(range.startLineNo, range.startColNo)
        val endLookup = lookupNodeAtEditorPos(range.endLineNo, range.endColNo)

        if (startLookup == null || endLookup == null) return ""

        return getContentBetweenNodeLookups(startLookup, endLookup)
    }

    private fun getContentBetweenNodeLookups(startLookup: NodeLookupResult, endLookupResult: NodeLookupResult): String {
        if (startLookup.node == endLookupResult.node) {
            val node = startLookup.node
            val chunk = lookupChunkByDescriptor(node.piece.chunkDesc)
            val startOffset = getOffsetInChunk(node.piece.chunkDesc, node.piece.chunkStartPos)
            return chunk.chunkSubstring(startOffset + startLookup.pieceOffset, startOffset + endLookupResult.pieceOffset)
        }

        var travNode = startLookup.node
        val chunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)
        val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)

        val res = StringBuilder()

        res.append(chunk.chunkSubstring(startOffset + startLookup.pieceOffset, startOffset + travNode.piece.textLen))

        travNode = travNode.next
        while (travNode != SENTINEL) {
            val travChunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)
            val travStartOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)

            if (travNode == endLookupResult.node) {
                res.append(travChunk.chunkSubstring(travStartOffset, travStartOffset + endLookupResult.pieceOffset))
                break
            } else {
                res.append(chunk.chunkSubstring(travStartOffset, travNode.piece.textLen))
            }

            travNode = travNode.next
        }

        return res.toString()
    }

    fun getLinesRawContent(): String {
        val resBuilder = StringBuilder()

        var travNode = myRoot.leftmostChild
        var totalTextLen = 0

        while (totalTextLen != myTextLength) {
            val chunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)
            val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)

            resBuilder.append(chunk.chunkSubstring(startOffset, startOffset + travNode.piece.textLen))
            totalTextLen += travNode.piece.textLen

            travNode = travNode.next
        }

        return resBuilder.toString()
    }

    fun getLineContent(lineNo: Int): String {
        if (myLastVisitedLineNo == lineNo) {
            return myLastVisitedLineValue
        }

        myLastVisitedLineNo = lineNo

        // This will need a rewrite if I ever want to add some \r\n handling
        myLastVisitedLineValue = getLineRawContent(lineNo)

        return myLastVisitedLineValue
    }

    fun getLineRawContent(lineNo: Int, endOffset: Int = 0): String {
        var travNode = myRoot
        val returnBuffer = StringBuilder()

        val cacheEntry = mySearchCache.getEntryByLineNumber(lineNo)
        // If the cache lookup is successful, figure out how to return it.
        if (cacheEntry != null) {
            travNode = cacheEntry.node
            val prevAccumulatedValue = travNode.getOffsetByLineStart(
                lineNo - (cacheEntry.nodeStartLineNumber ?: 0) - 1
            )
            val chunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)
            val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)
            if ((cacheEntry.nodeStartLineNumber ?: 0) + travNode.piece.lineFeeds == lineNo) {
                returnBuffer.append(
                    chunk.chunkSubstring(
                        startOffset + prevAccumulatedValue,
                        startOffset + travNode.piece.textLen
                    )
                )
            } else {
                val accumulatedValue = travNode.getOffsetByLineStart(lineNo - cacheEntry.nodeStartOffset)
                return chunk.chunkSubstring(
                    startOffset + prevAccumulatedValue,
                    startOffset + accumulatedValue - endOffset
                )
            }
        } else {
            // Else we try to go down from the root
            var travNodeStartOffset = 0
            var travNodeLineNo = lineNo

            while (travNode != SENTINEL) {
                if (travNode.left != SENTINEL && travNode.lineFeedsLeft >= travNodeLineNo - 1) {
                    travNode = travNode.left
                } else if (travNode.lineFeedsLeft + travNode.piece.lineFeeds > travNodeLineNo - 1) {
                    val prevMemorizedOffset = travNode.getOffsetByLineStart(
                        travNodeLineNo - travNode.lineFeedsLeft - 2
                    )
                    val memorizedOffset = travNode.getOffsetByLineStart(
                        travNodeLineNo - travNode.lineFeedsLeft - 1
                    )
                    val chunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)
                    val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)
                    travNodeStartOffset += travNode.textLenLeft

                    mySearchCache.addEntry(
                        PieceTreeSearchCacheEntry(
                            travNode,
                            travNodeStartOffset,
                            lineNo - (travNodeLineNo - 1 - travNode.lineFeedsLeft)
                        )
                    )

                    return chunk.chunkSubstring(
                        startOffset + prevMemorizedOffset,
                        startOffset + travNode.piece.textLen
                    )
                } else if (travNode.lineFeedsLeft + travNode.piece.lineFeeds == travNodeLineNo - 1) {
                    val prevAccumulatedValue =
                        travNode.getOffsetByLineStart(travNodeLineNo - travNode.lineFeedsLeft - 2)
                    val chunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)
                    val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)

                    returnBuffer.clear()
                    returnBuffer.append(
                        chunk.chunkSubstring(
                            startOffset + prevAccumulatedValue,
                            startOffset + travNode.piece.textLen
                        )
                    )
                    break
                } else {
                    travNodeLineNo -= travNode.lineFeedsLeft + travNode.piece.lineFeeds
                    travNodeStartOffset += travNode.textLenLeft + travNode.piece.textLen
                    travNode = travNode.right
                }
            }
        }

        // And if we only found the begging of the line, we must iterate via node.next
        travNode = travNode.next
        while (travNode != SENTINEL) {
            val chunk = lookupChunkByDescriptor(travNode.piece.chunkDesc)

            if (travNode.piece.lineFeeds > 0) {
                val accumulatedValue = travNode.getOffsetByLineStart(0)
                val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)

                returnBuffer.append(chunk.chunkSubstring(startOffset, startOffset + accumulatedValue))
                return returnBuffer.toString()
            } else {
                val startOffset = getOffsetInChunk(travNode.piece.chunkDesc, travNode.piece.chunkStartPos)
                returnBuffer.append(chunk.chunkSubstring(startOffset, startOffset + travNode.piece.textLen))
            }

            travNode = travNode.next
        }

        return returnBuffer.toString()
    }

    fun delete(offset: Int, cnt: Int) {
        myLastVisitedLineNo = 0
        myLastVisitedLineValue = ""

        if (cnt <= 0 || myRoot == SENTINEL) return

        val startLookup = lookupNodeAtOffset(offset)
        val endLookup = lookupNodeAtOffset(offset)

        if (startLookup == null || endLookup == null) {
            // Logger.WARN
            return
        }

        val startNode = startLookup.node
        val endNode = endLookup.node

        if (startNode == endNode) {
            val startSplitPos = getEditorCursorPosition(startNode.piece, startLookup.pieceOffset)
            val endSplitPos = getEditorCursorPosition(endNode.piece, endLookup.pieceOffset)

            if (startLookup.nodeStartOffset == offset) {
                if (cnt == startNode.piece.textLen) {
                    rbDelete(startNode)
                    recomputeGlobalTextMetadata()
                    return
                }
                startNode.deleteHead(endSplitPos)
                mySearchCache.validate(offset)
                recomputeGlobalTextMetadata()
                return
            }

            if (startLookup.nodeStartOffset + startNode.piece.textLen == offset + cnt) {
                startNode.deleteTail(startSplitPos)
                recomputeGlobalTextMetadata()
                return
            }

            // Otherwise, we should shrink a node (it is done with splitting)
            startNode.splitToRight(startSplitPos, endSplitPos)
            recomputeGlobalTextMetadata()
            return
        }

        // The text we want to delete spans some nodes
        val nodesToDelete = mutableListOf<PieceTreeNode>()

        val startSplitPos = getEditorCursorPosition(startNode.piece, startLookup.pieceOffset)
        startNode.deleteTail(startSplitPos)
        mySearchCache.validate(offset)
        if (startNode.piece.textLen == 0) {
            nodesToDelete.add(startNode)
        }

        val endSplitPosInBuffer = getEditorCursorPosition(endNode.piece, endLookup.pieceOffset)
        endNode.deleteHead(endSplitPosInBuffer)
        if (endNode.piece.textLen == 0) {
            nodesToDelete.add(endNode)
        }

        // We should delete the nodes in between
        var travNode = startNode.next
        while (travNode != SENTINEL) {
            nodesToDelete.add(travNode)
            travNode = travNode.next
        }

        rbDelete(nodesToDelete)
        recomputeGlobalTextMetadata()
    }

    fun insert(str: String, atOffset: Int, isEolNormalized: Boolean) {
        myIsEolNormalized = myIsEolNormalized && isEolNormalized

        myLastVisitedLineNo = 0
        myLastVisitedLineValue = ""

        if (myRoot != SENTINEL) {
            val lookupRes = lookupNodeAtOffset(atOffset)
                ?: // LOGGER.error()
                return

            val (node, nodeStartOffset, pieceOffset) = lookupRes
            val piece = node.piece
            val chunkDesc = piece.chunkDesc
            val insertPosInEditor = getEditorCursorPosition(piece, pieceOffset)

            if (chunkDesc.bufferKind == PieceChunkKind.DIFF
                && piece.editorEnd.lineNo == myLastChangedLineNo
                && piece.editorEnd.colNo == myLastChangedColNo
                && nodeStartOffset + piece.textLen == atOffset
                && str.length < PREFERRED_PIECE_TREE_CHUNK_SIZE
            ) { // Just append to the buffer
                node.appendString(str)
                recomputeGlobalTextMetadata()
            }

            if (nodeStartOffset == atOffset) {
                node.insertStringToLeftOfThis(str)
            } else if (nodeStartOffset + node.piece.textLen > atOffset) {
                // Insert into the middle
                val newRightPiece = Piece(
                    piece.chunkDesc,
                    insertPosInEditor,
                    piece.editorEnd,
                    getLineFeedCnt(piece.chunkDesc, insertPosInEditor, piece.editorEnd),
                    getOffsetInChunk(piece.chunkDesc, piece.editorEnd)
                            - getOffsetInChunk(piece.chunkDesc, insertPosInEditor)
                )

                node.deleteTail(insertPosInEditor)

                val newPieces = commitStringToChunkAndMakePieces(str)
                var tmpNode = node
                if (newRightPiece.textLen > 0) {
                    rbInsertNodeFromPieceRight(tmpNode, newRightPiece)
                }
                for (p in newPieces) {
                    tmpNode = rbInsertNodeFromPieceRight(tmpNode, p)
                }

            } else {
                node.insertStringToRightOfThis(str)
            }
        } else {
            val pieces = commitStringToChunkAndMakePieces(str)
            var node = rbInsertNodeFromPieceLeft(null, pieces[0])

            for (i in 1 until pieces.size) {
                node = rbInsertNodeFromPieceRight(node, pieces[i])
            }
        }

        recomputeGlobalTextMetadata()
    }

    private fun getOffsetInChunk(bufferDescriptor: PieceChunkDescriptor, editorPos: PieceChunkLineColOffset): Int {
        val lineStartOffsets = lookupChunkByDescriptor(bufferDescriptor).lineStartOffsets
        return lineStartOffsets[editorPos.lineNo] + editorPos.colNo
    }

    /**
     * Big [str]s become [FileChunk]s, and the small ones go to the current [DiffChunk].
     */
    private fun commitStringToChunkAndMakePieces(str: String): List<Piece> {
        val newPieces = mutableListOf<Piece>()
        if (str.length > PREFERRED_PIECE_TREE_CHUNK_SIZE) {
            var text = str
            while (text.length > PREFERRED_PIECE_TREE_CHUNK_SIZE) {
                val splitText: String = text.substring(0, PREFERRED_PIECE_TREE_CHUNK_SIZE)
                text = text.substring(PREFERRED_PIECE_TREE_CHUNK_SIZE)

                val lineStartOffsets = splitText.getLineStartOffsetsList()
                newPieces.add(
                    Piece(
                        PieceChunkDescriptor(PieceChunkKind.FILE, myFileChunks.size),
                        PieceChunkLineColOffset(0, 0),
                        PieceChunkLineColOffset(lineStartOffsets.lastIndex, splitText.length - lineStartOffsets.last()),
                        lineStartOffsets.lastIndex,
                        splitText.length
                    )
                )
                myFileChunks.add(FileChunk(splitText, lineStartOffsets))
            }

            // Add the last one
            val lineStartOffsets = text.getLineStartOffsetsList()
            newPieces.add(
                Piece(
                    PieceChunkDescriptor(PieceChunkKind.FILE, myFileChunks.size),
                    PieceChunkLineColOffset(0, 0),
                    PieceChunkLineColOffset(lineStartOffsets.lastIndex, text.length - lineStartOffsets.last()),
                    lineStartOffsets.lastIndex,
                    text.length
                )
            )
            myFileChunks.add(FileChunk(text, lineStartOffsets))

            return newPieces
        }

        return listOf(makeSingleDiffPiece(str))
    }

    private fun makeSingleDiffPiece(str: String): Piece {
        val lastDiffChunk = myDiffChunks.last()
        val startOffset = lastDiffChunk.chunkLength
        val lineStartOffsets = str.getLineStartOffsetsList()
        for (i in lineStartOffsets.indices) {
            lineStartOffsets[i] += startOffset
        }

        lastDiffChunk.lineStartOffsets = lastDiffChunk.lineStartOffsets + lineStartOffsets
        lastDiffChunk.commit(str)

        val endOffset = lastDiffChunk.chunkLength
        val endLine = lastDiffChunk.lineStartOffsets.lastIndex
        val endCol = endOffset - lastDiffChunk.lineStartOffsets[endLine]
        val endPos = PieceChunkLineColOffset(endLine, endCol)
        val chunkDesc = PieceChunkDescriptor(PieceChunkKind.DIFF, 0)
        val startPos = PieceChunkLineColOffset(myLastChangedLineNo, myLastChangedColNo)
        val newPiece = Piece(
            chunkDesc,
            startPos,
            endPos,
            getLineFeedCnt(chunkDesc, startPos, endPos),
            endOffset - startOffset
        )

        myLastChangedLineNo = endPos.lineNo
        myLastChangedColNo = endPos.colNo

        return newPiece
    }


    /**
     * This function will need an overhaul to support \r\n line endings.
     */
    private fun getLineFeedCnt(
        chunkDescriptor: PieceChunkDescriptor,
        start: PieceChunkLineColOffset,
        end: PieceChunkLineColOffset
    ) = end.lineNo - start.lineNo

    private fun lookupNodeAtEditorPos(lineNo: Int, colNo: Int): NodeLookupResult? {
        var travNode = myRoot

        var nodeStartOffset = 0
        var travLineNo = lineNo
        var travColNo = colNo

        while (travNode != SENTINEL) {
            if (travNode.left != SENTINEL && travNode.lineFeedsLeft >= travLineNo - 1) {
                travNode = travNode.left
            } else if (travNode.lineFeedsLeft + travNode.piece.lineFeeds > travLineNo - 1) {
                val prevLineStartOffset = travNode.getOffsetByLineStart(travLineNo - travNode.lineFeedsLeft - 2)
                val lineStartOffset = travNode.getOffsetByLineStart(travLineNo - travNode.lineFeedsLeft - 1)
                nodeStartOffset += travNode.textLenLeft

                return NodeLookupResult(
                    travNode,
                    nodeStartOffset,
                    (prevLineStartOffset + travColNo - 1).coerceAtMost(lineStartOffset)
                )
            } else if (travNode.lineFeedsLeft + travNode.piece.lineFeeds == travLineNo - 1) {
                val prevLineStartOffset = travNode.getOffsetByLineStart(travLineNo - travNode.lineFeedsLeft - 2)
                if (prevLineStartOffset + travColNo - 1 <= travNode.piece.textLen) {
                    return NodeLookupResult(
                        travNode,
                        nodeStartOffset,
                        prevLineStartOffset + travColNo - 1
                    )
                } else {
                    travColNo -= travNode.piece.textLen - prevLineStartOffset;
                    break
                }
            } else {
                travLineNo -= travNode.lineFeedsLeft + travNode.piece.lineFeeds
                nodeStartOffset += travNode.textLenLeft + travNode.piece.textLen
                travNode = travNode.right
            }
        }

        // Search in order to find the node that contains the colNo
        travNode = travNode.next
        while (travNode != SENTINEL) {

            if (travNode.piece.lineFeeds > 0) {
                val beginningLineStartOffset = travNode.getOffsetByLineStart(0)
                return NodeLookupResult(
                    travNode,
                    travNode.getNodeStartOffset(),
                    (travColNo - 1).coerceAtMost(beginningLineStartOffset)
                )
            } else {
                if (travNode.piece.textLen >= travColNo - 1) {
                    return NodeLookupResult(
                        travNode,
                        travNode.getNodeStartOffset(),
                        travColNo - 1
                    )
                } else {
                    travColNo -= travNode.piece.textLen;
                }
            }

            travNode = travNode.next
        }

        return null
    }

    private fun lookupNodeAtOffset(offset: Int): NodeLookupResult? {
        var travRoot = this.myRoot

        val cacheEntry = mySearchCache.getEntryByOffset(offset)
        if (cacheEntry != null) return cacheEntry.let {
            NodeLookupResult(it.node, it.nodeStartOffset, offset - it.nodeStartOffset)
        }

        var nodeStartOffset = 0
        var mutOffset = offset

        while (travRoot != SENTINEL) {
            if (travRoot.textLenLeft > mutOffset) {
                // We need to go towards the beginning
                travRoot = travRoot.left
            } else if (travRoot.textLenLeft + travRoot.piece.textLen >= mutOffset) {
                // We are in the proper node
                nodeStartOffset += travRoot.textLenLeft
                val result = NodeLookupResult(
                    travRoot,
                    mutOffset - travRoot.textLenLeft,
                    nodeStartOffset
                )

                mySearchCache.addEntry(result.let {
                    PieceTreeSearchCacheEntry(it.node, it.nodeStartOffset, null)
                })

                return result
            } else {
                // Going right is a little painful
                val fullLeftOffset = travRoot.textLenLeft + travRoot.piece.textLen
                mutOffset -= fullLeftOffset
                nodeStartOffset += fullLeftOffset
                travRoot = travRoot.right
            }
        }

        return null
    }

    private fun lookupChunkByDescriptor(desc: PieceChunkDescriptor) = when (desc.bufferKind) {
        PieceChunkKind.FILE -> myFileChunks[desc.bufferIndex]
        PieceChunkKind.DIFF -> myDiffChunks[desc.bufferIndex]
    }

    private fun getEditorCursorPosition(piece: Piece, pieceOffset: Int): PieceChunkLineColOffset {
        val lineStartOffsets = lookupChunkByDescriptor(piece.chunkDesc).lineStartOffsets
        val startOffset = lineStartOffsets[piece.chunkStartPos.lineNo] + piece.chunkStartPos.colNo

        val globalOffset = startOffset + pieceOffset

        // Binary search between start offset and end offset
        var lowLineNo = piece.chunkStartPos.lineNo
        var highLineNo = piece.chunkStartPos.lineNo

        var midLineNo = 0
        var midOffsetStop: Int
        var midOffsetStart = 0

        while (lowLineNo <= highLineNo) {
            midLineNo = lowLineNo + ((highLineNo - lowLineNo) / 2)

            if (midLineNo == highLineNo) break

            midOffsetStart = lineStartOffsets[midLineNo]
            midOffsetStop = lineStartOffsets[midLineNo + 1]

            if (globalOffset < midOffsetStart) {
                highLineNo = midLineNo - 1
            } else if (globalOffset >= midOffsetStop) {
                lowLineNo = midLineNo + 1
            } else {
                break
            }
        }

        return PieceChunkLineColOffset(midLineNo, globalOffset - midOffsetStart)
    }

    //region CRLF handling (that ensures that a \r\n sequence is never split)

    // TODO @thisisvolatile This code will be here if CRLF support is ever needed.

    //endregion

    //region PieceTree-specific node methods

    private fun PieceTreeNode.splitToRight(thisNewEnd: PieceChunkLineColOffset, otherNewStart: PieceChunkLineColOffset) {
        val originalStartPos = piece.chunkStartPos
        val originalEndPos = piece.editorEnd

        // From [originalStartPos] to [thisNewEnd]
        val oldLength = piece.textLen
        val oldLFCnt = piece.lineFeeds
        val newLineFeedCnt = getLineFeedCnt(piece.chunkDesc, piece.chunkStartPos, thisNewEnd)
        val newLength =
            getOffsetInChunk(piece.chunkDesc, thisNewEnd) - getOffsetInChunk(piece.chunkDesc, originalStartPos)

        piece = Piece(
            piece.chunkDesc,
            originalStartPos,
            thisNewEnd,
            newLineFeedCnt,
            newLength
        )

        updateSubtreeTextMetadata(newLength - oldLength, newLineFeedCnt - oldLFCnt)

        // From [otherNewStart] to [originalEndPos]
        val newPiece = Piece(
            piece.chunkDesc,
            otherNewStart,
            originalEndPos,
            getLineFeedCnt(piece.chunkDesc, otherNewStart, originalEndPos),
            getOffsetInChunk(piece.chunkDesc, originalEndPos) - getOffsetInChunk(piece.chunkDesc, otherNewStart)
        )

        rbInsertNodeFromPieceRight(this, newPiece)
    }

    private fun PieceTreeNode.deleteTail(fromEditorPos: PieceChunkLineColOffset) {
        val originalLFCnt = piece.lineFeeds
        val originalEndOffset = getOffsetInChunk(piece.chunkDesc, piece.editorEnd)

        val newEndOffset = getOffsetInChunk(piece.chunkDesc, fromEditorPos)
        val newLineFeedCnt = getLineFeedCnt(piece.chunkDesc, piece.chunkStartPos, fromEditorPos)

        val lineFeedsDelta = newLineFeedCnt - originalLFCnt
        val textLengthDelta = newEndOffset - originalEndOffset
        val newLength = piece.textLen + textLengthDelta

        piece = Piece(
            piece.chunkDesc,
            piece.chunkStartPos,
            fromEditorPos,
            newLineFeedCnt,
            newLength
        )

        updateSubtreeTextMetadata(textLengthDelta, lineFeedsDelta)
    }

    private fun PieceTreeNode.getOffsetByLineStart(lineStartOffset: Int): Int {
        if (lineStartOffset < 0) return 0
        val lineStarts = lookupChunkByDescriptor(piece.chunkDesc).lineStartOffsets
        val expectedLineStartIndex = piece.chunkStartPos.lineNo + lineStartOffset + 1
        return if (expectedLineStartIndex > piece.editorEnd.lineNo) {
            lineStarts[piece.editorEnd.lineNo] + piece.editorEnd.colNo -
                    (lineStarts[piece.chunkStartPos.lineNo] + piece.chunkStartPos.colNo) //TODO make it prettier
        } else {
            lineStarts[expectedLineStartIndex] - (lineStarts[piece.chunkStartPos.lineNo] + piece.chunkStartPos.colNo)
        }
    }

    private fun PieceTreeNode.deleteHead(fromPos: PieceChunkLineColOffset) {
        val originalLFCnt = piece.lineFeeds
        val originalStartOffset = getOffsetInChunk(piece.chunkDesc, piece.chunkStartPos)

        val newLineFeedCnt = getLineFeedCnt(piece.chunkDesc, fromPos, piece.editorEnd)
        val newStartOffset = getOffsetInChunk(piece.chunkDesc, fromPos)
        val lineFeedsDelta = newLineFeedCnt - originalLFCnt
        val textLenDelta = originalStartOffset - newStartOffset
        val newLength = piece.textLen + textLenDelta
        piece = Piece(
            piece.chunkDesc,
            fromPos,
            piece.editorEnd,
            newLineFeedCnt,
            newLength
        )

        updateSubtreeTextMetadata(textLenLeft, lineFeedsDelta)
    }

    private fun PieceTreeNode.insertStringToLeftOfThis(str: String) {
        val newPieces = commitStringToChunkAndMakePieces(str)
        var tmpNode = this
        for (piece in newPieces.reversed()) {
            tmpNode = rbInsertNodeFromPieceLeft(tmpNode, piece)
        }
    }

    private fun PieceTreeNode.insertStringToRightOfThis(str: String) {
        val newPieces = commitStringToChunkAndMakePieces(str)
        var tmpNode = this
        for (piece in newPieces) {
            tmpNode = rbInsertNodeFromPieceRight(tmpNode, piece)
        }
    }

    private fun PieceTreeNode.appendString(str: String) {
        val lastDiffChunk = myDiffChunks.last()
        val startOffset = lastDiffChunk.chunkLength

        lastDiffChunk.commit(str)

        val lineStartOffsets = str.getLineStartOffsetsList()
        for (i in lineStartOffsets.indices) {
            lineStartOffsets[i] += startOffset
        }


        lastDiffChunk.lineStartOffsets = lastDiffChunk.lineStartOffsets + lineStartOffsets
        val newEnd = PieceChunkLineColOffset(
            lastDiffChunk.lineStartOffsets.last(),
            lastDiffChunk.chunkLength - lastDiffChunk.lineStartOffsets.last()
        )
        val newLength = piece.textLen + str.length

        val oldLineFeedsCnt = piece.lineFeeds
        val newLineFeedsCnt = getLineFeedCnt(piece.chunkDesc, piece.chunkStartPos, newEnd)
        val lineFeedDelta = newLineFeedsCnt - oldLineFeedsCnt

        piece = Piece(
            piece.chunkDesc,
            piece.chunkStartPos,
            newEnd,
            newLineFeedsCnt,
            newLength
        )

        myLastChangedLineNo = newEnd.lineNo
        myLastChangedColNo = newEnd.colNo

        updateSubtreeTextMetadata(str.length, lineFeedDelta)
    }

    private fun PieceTreeNode.getNodeStartOffset(): Int {
        var travNode = this
        var resOffset = textLenLeft

        while (travNode != myRoot) {
            if (travNode.isRightChild) {
                resOffset += travNode.parent.textLenLeft + travNode.parent.piece.textLen
            }
            travNode = travNode.parent
        }

        return resOffset
    }

    //endregion

    //region The Red-Black tree structure

    // ASCII graphics courtesy of Microsoft, Peng Lyu, MIT licenced.
    // The RB tree is implemented as described in Corman's book on data structures.

    /**
     *      node              node
     *     /  \              /  \
     *    a   b    ---->   a    b
     *                         /
     *                        z
     */
    private fun rbInsertNodeFromPieceRight(node: PieceTreeNode?, piece: Piece): PieceTreeNode {
        val z = PieceTreeNode(NodeColor.Red, piece)

        if (myRoot == SENTINEL) {
            myRoot = z
            z.apply {
                parent = SENTINEL
                left = SENTINEL
                right = SENTINEL
            }
            z.color = NodeColor.Black
        } else if (node?.right == SENTINEL) {
            node.right = z
            z.parent = node
            z.apply {
                left = SENTINEL
                right = SENTINEL
            }
        } else {
            val nextNode = node?.right?.leftmostChild
            if (nextNode != null) {
                nextNode.left = z
                z.parent = nextNode
                z.apply {
                    left = SENTINEL
                    right = SENTINEL
                }
            }
        }

        fixInsert(z)
        return z
    }

    /**
     *      node              node
     *     /  \              /  \
     *    a   b     ---->   a    b
     *                       \
     *                        z
     */
    private fun rbInsertNodeFromPieceLeft(node: PieceTreeNode?, piece: Piece): PieceTreeNode {
        val z = PieceTreeNode(NodeColor.Red, piece)

        if (myRoot == SENTINEL) {
            myRoot = z
            z.apply {
                parent = SENTINEL
                left = SENTINEL
                right = SENTINEL
            }
            z.color = NodeColor.Black
        } else if (node?.left == SENTINEL) {
            node.left = z
            z.parent = node
            z.apply {
                left = SENTINEL
                right = SENTINEL
            }
        } else {
            val prevNode = node?.left?.rigthmostChild
            if (prevNode != null) {
                prevNode.right = z
                z.parent = prevNode
                z.apply {
                    left = SENTINEL
                    right = SENTINEL
                }
            }
        }

        fixInsert(z)
        return z
    }

    private fun fixInsert(x: PieceTreeNode) {
        var fixingFor = x
        fixingFor.recomputeSubtreeTextMetadata()

        while (fixingFor != myRoot && fixingFor.parent.color == NodeColor.Red) {
            if (fixingFor.parent.isLeftChild) {
                val rightUncle = fixingFor.grandparent.right

                if (rightUncle.color == NodeColor.Red) {
                    fixingFor.parent.color = NodeColor.Black
                    rightUncle.color = NodeColor.Black
                    fixingFor.grandparent.color = NodeColor.Red
                    fixingFor = fixingFor.grandparent
                } else {
                    if (fixingFor.isRightChild) {
                        fixingFor = fixingFor.parent
                        leftRotate(fixingFor)
                    }

                    fixingFor.parent.color = NodeColor.Black
                    fixingFor.grandparent.color = NodeColor.Red
                    rightRotate(fixingFor.grandparent)
                }
            } else {
                val leftUncle = fixingFor.parent.parent.left

                if (leftUncle.color == NodeColor.Red) {
                    fixingFor.parent.color = NodeColor.Black
                    leftUncle.color = NodeColor.Black
                    fixingFor.parent.parent.color = NodeColor.Red
                    fixingFor = fixingFor.parent.parent
                } else {
                    if (fixingFor.isLeftChild) {
                        fixingFor = fixingFor.parent
                        rightRotate(fixingFor)
                    }
                    fixingFor.parent.color = NodeColor.Black
                    fixingFor.parent.parent.color = NodeColor.Red
                    leftRotate(fixingFor.parent.parent)
                }
            }
        }

        myRoot.color = NodeColor.Black
    }

    private fun leftRotate(at: PieceTreeNode) {
        val rightChild = at.right

        // Fix metadata
        rightChild.textLenLeft += at.textLenLeft + at.piece.textLen
        rightChild.lineFeedsLeft += at.lineFeedsLeft + at.piece.lineFeeds
        at.right = rightChild.left

        if (rightChild.left != SENTINEL) {
            rightChild.left.parent = at
        }
        rightChild.parent = at.parent
        if (at.parent == SENTINEL) {
            myRoot = rightChild
        } else if (at.isLeftChild) {
            at.parent.left = rightChild
        } else {
            at.parent.right = rightChild
        }

        rightChild.left = at
        at.parent = rightChild
    }

    private fun rightRotate(about: PieceTreeNode) {
        val leftChild = about.left
        about.left = leftChild.right
        if (leftChild.right != SENTINEL) {
            leftChild.right.parent = about
        }
        leftChild.parent = about.parent

        // Fix precalculated sizes
        about.textLenLeft -= leftChild.textLenLeft + leftChild.piece.textLen
        about.lineFeedsLeft -= leftChild.lineFeedsLeft + leftChild.piece.lineFeeds

        if (about.parent == SENTINEL) {
            myRoot = leftChild
        } else if (about == about.parent.right) {
            about.parent.right = leftChild
        } else {
            about.parent.left = leftChild
        }

        leftChild.right = about
        about.parent = leftChild
    }

    private fun rbDelete(nodes: List<PieceTreeNode>) = nodes.forEach { rbDelete(it) }

    private fun rbDelete(z: PieceTreeNode) {
        val x: PieceTreeNode
        val y: PieceTreeNode

        if (z.left == SENTINEL) {
            y = z
            x = y.right
        } else if (z.right == SENTINEL) {
            y = z
            x = y.left
        } else {
            y = z.right.leftmostChild
            x = y.right
        }

        if (y == myRoot) {
            myRoot = x

            // if x is null, we are removing the only node
            x.color = NodeColor.Black
            z.detach()
            SENTINEL.detach()
            myRoot.parent = SENTINEL

            return
        }

        val yWasRed = (y.color == NodeColor.Red)

        if (y.isLeftChild) {
            y.parent.left = x
        } else {
            y.parent.right = x
        }

        if (y == z) {
            x.parent = y.parent
            x.recomputeSubtreeTextMetadata()
        } else {
            if (y.parent == z) {
                x.parent = y
            } else {
                x.parent = y.parent
            }

            // as we make changes to x's hierarchy, update size_left of subtree first
            x.recomputeSubtreeTextMetadata()

            y.left = z.left
            y.right = z.right
            y.parent = z.parent
            y.color = z.color

            if (z == myRoot) {
                myRoot = y
            } else {
                if (z.isLeftChild) {
                    z.parent.left = y
                } else {
                    z.parent.right = y
                }
            }

            if (y.left != SENTINEL) {
                y.left.parent = y
            }
            if (y.right != SENTINEL) {
                y.right.parent = y
            }
            // update metadata
            // we replace z with y, so in this sub-tree, the length change is z.item.length
            y.textLenLeft = z.textLenLeft
            y.lineFeedsLeft = z.lineFeedsLeft
            y.recomputeSubtreeTextMetadata()
        }

        z.detach()

        if (x.isLeftChild) {
            val newSizeLeft = x.subtreeTextLen
            val newLFLeft = x.subtreeLineFeedsCount
            if (newSizeLeft != x.parent.textLenLeft || newLFLeft != x.parent.lineFeedsLeft) {
                val textLenDelta = newSizeLeft - x.parent.textLenLeft
                val lineFeedsDelta = newLFLeft - x.parent.lineFeedsLeft
                x.parent.textLenLeft = newSizeLeft
                x.parent.lineFeedsLeft = newLFLeft
                x.parent.updateSubtreeTextMetadata(textLenDelta, lineFeedsDelta)
            }
        }

        x.parent.recomputeSubtreeTextMetadata()

        if (yWasRed) {
            SENTINEL.detach()
            return
        }

        fixDelete(x)
    }

    private fun fixDelete(x: PieceTreeNode) {
        var xVar = x

        var w: PieceTreeNode
        while (xVar != myRoot && xVar.color == NodeColor.Black) {
            if (xVar == xVar.parent.left) {
                w = xVar.parent.right

                if (w.color == NodeColor.Red) {
                    w.color = NodeColor.Black
                    x.parent.color = NodeColor.Red
                    leftRotate(x.parent)
                    w = xVar.parent.right
                }

                if (w.left.color == NodeColor.Black && w.right.color == NodeColor.Black) {
                    w.color = NodeColor.Red
                    xVar = xVar.parent
                } else {
                    if (w.right.color == NodeColor.Black) {
                        w.left.color = NodeColor.Black
                        w.color = NodeColor.Red
                        rightRotate(w)
                        w = x.parent.right
                    }

                    w.color = x.parent.color
                    xVar.parent.color = NodeColor.Black
                    w.right.color = NodeColor.Black
                    leftRotate(xVar.parent)
                    xVar = myRoot
                }
            } else {
                w = xVar.parent.left

                if (w.color == NodeColor.Red) {
                    w.color = NodeColor.Black
                    xVar.parent.color = NodeColor.Red
                    rightRotate(xVar.parent)
                    w = xVar.parent.left
                }

                if (w.left.color == NodeColor.Black && w.right.color == NodeColor.Black) {
                    w.color = NodeColor.Red
                    xVar = xVar.parent

                } else {
                    if (w.left.color == NodeColor.Black) {
                        w.right.color = NodeColor.Black
                        w.color = NodeColor.Red
                        leftRotate(w)
                        w = x.parent.left
                    }

                    w.color = x.parent.color
                    xVar.parent.color = NodeColor.Black
                    w.left.color = NodeColor.Black
                    rightRotate(x.parent)
                    xVar = myRoot
                }
            }
        }
        xVar.color = NodeColor.Black
        SENTINEL.detach()
    }

    //endregion

    //region Update/recompute metadata routines

    private fun PieceTreeNode.updateSubtreeTextMetadata(deltaTextLenLeft: Int, deltaLineFeedsCnt: Int) {
        var travNode = this
        while (travNode != myRoot && travNode != SENTINEL) {
            if (travNode.isLeftChild) {
                travNode.parent.textLenLeft += deltaTextLenLeft
                travNode.parent.lineFeedsLeft += deltaLineFeedsCnt
            }
            travNode = travNode.parent
        }
    }

    private fun PieceTreeNode.recomputeSubtreeTextMetadata() {
        val textLengthDelta: Int
        val lineFeedsCntDelta: Int
        var travNode = this

        if (travNode == myRoot) return

        while (travNode != myRoot && travNode.isRightChild) {
            travNode = travNode.parent
        }

        if (travNode == myRoot) return

        // travNode is the root of the changed subtree
        travNode = travNode.parent
        textLengthDelta = travNode.left.subtreeTextLen - travNode.textLenLeft
        lineFeedsCntDelta = travNode.left.subtreeLineFeedsCount - travNode.lineFeedsLeft

        travNode.textLenLeft += textLengthDelta
        travNode.lineFeedsLeft += lineFeedsCntDelta

        // Warn if deltas are 0
        // TODO Logger.warn

        // Upwards, towards the root
        while (travNode != myRoot) {
            if (travNode.isLeftChild) {
                travNode.parent.textLenLeft += textLengthDelta
                travNode.parent.lineFeedsLeft += lineFeedsCntDelta
            }

            travNode = travNode.parent
        }
    }

    private fun recomputeGlobalTextMetadata() {
        var travRoot = myRoot
        var lineFeedsCnt = 1
        var textLength = 0

        while (travRoot != SENTINEL) {
            lineFeedsCnt += travRoot.lineFeedsLeft + travRoot.piece.lineFeeds
            textLength += travRoot.textLenLeft + travRoot.piece.textLen
            travRoot = travRoot.right
        }

        myLineFeedsCnt = lineFeedsCnt
        myTextLength = textLength
        mySearchCache.validate(textLength)
    }

    //endregion
}

//endregion
