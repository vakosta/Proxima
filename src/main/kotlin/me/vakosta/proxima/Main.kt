package me.vakosta.proxima

import androidx.compose.ui.window.application
import me.vakosta.proxima.domain.common.highlightsModule
import me.vakosta.proxima.domain.common.lifetimes.defineLifetime
import me.vakosta.proxima.presentation.states.MainWindowState
import me.vakosta.proxima.presentation.windows.MainWindow
import org.koin.core.context.startKoin
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.system.exitProcess

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
        val state = MainWindowState(mainWindowLifetimeDef.lifetime, onCloseRequest = {
            mainWindowLifetimeDef.terminateLifetime()
            exitApplication()
            exitProcess(0)
        })
        MainWindow(
            state = state
        )
    }
}
