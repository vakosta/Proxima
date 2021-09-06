package presentation.views

import androidx.compose.runtime.Composable
import presentation.model.CodeViewer
import presentation.views.editor.EditorView

@Composable
fun CodeViewerView(model: CodeViewer) {
    EditorView(model.editors.active!!, model.settings)
}
