package presentation.windows

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.codeviewer.ui.common.AppTheme
import org.jetbrains.codeviewer.ui.common.Settings
import presentation.model.CodeViewer
import presentation.views.CodeViewerView
import presentation.views.editor.Editors

@Composable
fun MainWindow() {
    val codeViewer = remember {
        val editors = Editors()

        CodeViewer(
            editors = editors,
            settings = Settings()
        )
    }

    DisableSelection {
        MaterialTheme(
            colors = AppTheme.colors.material
        ) {
            Surface {
                CodeViewerView(codeViewer)
            }
        }
    }
}
