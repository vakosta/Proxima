package ru.hse.hseditor

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.hse.hseditor.domain.common.lifetimes.defineLifetime
import ru.hse.hseditor.domain.common.highlightsModule
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.windows.MainWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.system.exitProcess

fun gracefulShutdown() {
    // Not so graceful for now
    exitProcess(0)
}

fun main() {
    // Some dark Swing magic
    SwingUtilities.invokeLater { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()) }
    // Some dark Koin magic
    startKoin {
        modules(
            highlightsModule,
        )
    }
    // Some dark lifetime magic
    val mainWindowLifetimeDef = defineLifetime("MainWindow Lifetime")

    application {
        val state = MainWindowState(mainWindowLifetimeDef.lifetime)
        MainWindow(
            state = state,
            onCloseRequest = {
                mainWindowLifetimeDef.terminateLifetime()
                exitApplication()
                gracefulShutdown()
            }
        )
    }
}
