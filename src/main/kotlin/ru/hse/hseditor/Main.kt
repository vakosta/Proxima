import androidx.compose.ui.window.application
import ru.hse.hseditor.presentation.states.MainWindowState
import ru.hse.hseditor.presentation.windows.MainWindow

fun main() = application {
    val state = MainWindowState()
    MainWindow(state)
}
