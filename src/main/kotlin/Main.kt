import androidx.compose.ui.window.application
import presentation.states.MainWindowState
import presentation.windows.MainWindow

fun main() = application {
    val state = MainWindowState()
    MainWindow(state)
}
