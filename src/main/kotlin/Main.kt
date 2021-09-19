import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import presentation.windows.MainWindow

fun main() = application {
    val state = rememberWindowState()
    MainWindow(state)
}
