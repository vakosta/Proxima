package ru.hse.hseditor.domain.common

import com.lodborg.intervaltree.IntervalTree
import org.koin.dsl.module
import ru.hse.hseditor.data.HighlightInterval
import ru.hse.hseditor.domain.highlights.TextState
import ru.hse.hseditor.domain.text.PieceTreeBuilder

val highlightsModule = module {
//    factory { PieceTreeBuilder().build() }
//    factory { IntervalTree<HighlightInterval>() }
//    factory {
//        TextState(
//            myLifetime = it.get(),
//            language = it.get(),
//            pieceTreeTextBuffer = get(),
//            highlights = get(),
//        )
//    }
}
