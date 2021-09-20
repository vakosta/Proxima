package presentation.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class PanelState {
    val splitter = SplitterState()
    var isExpanded by mutableStateOf(true)
    val collapsedSize = 24.dp
    var expandedSize by mutableStateOf(300.dp)
    val expandedSizeMin = 90.dp

    val panelSize: Dp
        get() = if (isExpanded)
            expandedSize
        else
            collapsedSize
}
