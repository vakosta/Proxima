package ru.hse.hseditor.domain.common

import com.lodborg.intervaltree.IntervalTree
import org.koin.dsl.module
import ru.hse.hseditor.data.HighlightInterval
import ru.hse.hseditor.domain.filesystem.FileSystemManager
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.domain.text.PieceTreeBuilder

val highlightsModule = module {
    single { PieceTreeBuilder().build() }
    single { IntervalTree<HighlightInterval>() }
    single {
        TextState(
            text = it.get(),
            language = it.get(),
            pieceTree = get(),
            highlights = get(),
        )
    }
}

val fileSystemModule = module {
    single { FileSystemManager() }
}
