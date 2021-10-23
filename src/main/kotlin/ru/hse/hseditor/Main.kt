package ru.hse.hseditor

import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.hse.hseditor.domain.common.lifetimes.defineLifetime
import ru.hse.hseditor.domain.common.highlightsModule
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.windows.MainWindow

fun main() {
    startKoin {
        modules(
            highlightsModule,
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
