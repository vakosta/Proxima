package ru.hse.hseditor

import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ru.hse.hseditor.domain.app.lifetimes.defineLifetime
import ru.hse.hseditor.domain.filesystem.FileSystemManager
import ru.hse.hseditor.domain.common.fileSystemModule
import ru.hse.hseditor.domain.common.highlightsModule
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.windows.MainWindow

val fileSystemModule = module {
    single { FileSystemManager() }
}

fun main() {
    startKoin {
        modules(
            highlightsModule,
            fileSystemModule,
        )
    }
    val mainWindowLifetimeDef = defineLifetime("MainWindow Lifetime")

    application {
        val state = MainWindowState(mainWindowLifetimeDef.lifetime)
        MainWindow(
            state = state,
            onCloseRequest = {
                mainWindowLifetimeDef.terminateLifetime()
                exitApplication()
            }
        )
    }
}
