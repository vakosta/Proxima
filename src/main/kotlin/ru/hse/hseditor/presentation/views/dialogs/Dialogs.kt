package ru.hse.hseditor.presentation.views.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.FrameWindowScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileFilter

enum class SwingFileDialogKind { OPEN, SAVE }

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun FrameWindowScope.SwingFileDialog(
    title: String,
    kind: SwingFileDialogKind,
    onPathChosen: (path: Path?) -> Unit,
    fileFilter: FileFilter? = null,
    fileSelectionModeInt: Int = JFileChooser.FILES_AND_DIRECTORIES,
) {
    DisposableEffect(Unit) {
        val job = GlobalScope.launch(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                this.fileSelectionMode = fileSelectionModeInt
                this.dialogTitle = title
                this.fileFilter = fileFilter
            }

            val result = when (kind) {
                SwingFileDialogKind.OPEN -> chooser.showOpenDialog(window)
                SwingFileDialogKind.SAVE -> chooser.showSaveDialog(window)
            }

            val path = when (result) {
                JFileChooser.APPROVE_OPTION -> chooser.selectedFile.toPath()
                else                        -> null
            }

            onPathChosen(path)
        }

        onDispose {
            job.cancel()
        }
    }
}

enum class SwingDialogResult { YES, NO, OK, CANCEL }

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun FrameWindowScope.SwingConfirmDialog(
    title: String,
    message: String,
    onOptionChosen: (result: SwingDialogResult) -> Unit
) {
    DisposableEffect(Unit) {
        val job = GlobalScope.launch(Dispatchers.Swing) {
            val resultInt = JOptionPane.showConfirmDialog(
                window, message, title, JOptionPane.YES_NO_CANCEL_OPTION
            )
            val result = when (resultInt) {
                JOptionPane.YES_OPTION -> SwingDialogResult.YES
                JOptionPane.NO_OPTION  -> SwingDialogResult.NO
                else                   -> SwingDialogResult.CANCEL
            }
            onOptionChosen(result)
        }

        onDispose {
            job.cancel()
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun FrameWindowScope.SwingAlertDialog(
    title: String,
    message: String,
    onOptionChosen: (result: SwingDialogResult) -> Unit
) {
    DisposableEffect(Unit) {
        val job = GlobalScope.launch(Dispatchers.Swing) {
            val resultInt = JOptionPane.showConfirmDialog(
                window, message, title, JOptionPane.OK_OPTION
            )
            val result = when (resultInt) {
                JOptionPane.OK_OPTION  -> SwingDialogResult.OK
                else                   -> SwingDialogResult.CANCEL
            }
            onOptionChosen(result)
        }

        onDispose {
            job.cancel()
        }
    }
}