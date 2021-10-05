package ru.hse.hseditor

import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.hse.hseditor.domain.common.fileSystemModule
import ru.hse.hseditor.domain.common.highlightsModule
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.windows.MainWindow

fun main() = application {
    startKoin {
        modules(
            highlightsModule,
            fileSystemModule,
        )
    }
    val state = MainWindowState()
    MainWindow(state)
}
