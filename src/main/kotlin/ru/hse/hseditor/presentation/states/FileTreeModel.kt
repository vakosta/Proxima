package ru.hse.hseditor.presentation.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.hse.hseditor.domain.common.lifetimes.defineLifetime
import ru.hse.hseditor.presentation.model.FileModel

class ExpandableFile(
    val file: FileModel,
    val level: Int,
) {

    // TODO fix lifetime management here.

    private val myLifetimeDef = defineLifetime("ExpandableLifetime")

    init {
        if (file.hasChildren) {
            file.children.onNewState.advise(myLifetimeDef.lifetime) {
                children.forEach { it.myLifetimeDef.terminateLifetime() }
                children = fileChildrenToExpandableChildren() // Repaint
            }
        }
    }

    private fun fileChildrenToExpandableChildren() = file.children
        .map { ExpandableFile(it, level + 1) }
        .sortedWith(compareBy({ it.file.isDirectory }, { it.file.name }))
        .sortedBy { !it.file.isDirectory }


    var children: List<ExpandableFile> by mutableStateOf(emptyList())
    val canExpand: Boolean get() = file.hasChildren

    fun toggleExpanded() {
        children = if (children.isEmpty()) {
            fileChildrenToExpandableChildren()
        } else {
            emptyList()
        }
    }
}

class FileTreeModel(
    val root: FileModel,
    val openFile: suspend (file: FileModel) -> Unit
) {
    private val expandableRoot = ExpandableFile(root, 0).apply { toggleExpanded() }

    val items: List<Item> get() = expandableRoot.toItems()

    inner class Item constructor(
        private val file: ExpandableFile
    ) {
        val name: String get() = file.file.name

        val level: Int get() = file.level

        val type: ItemType
            get() = if (file.file.isDirectory) {
                ItemType.Folder(isExpanded = file.children.isNotEmpty(), canExpand = file.canExpand)
            } else {
                ItemType.File(ext = file.file.name.substringAfterLast(".").lowercase())
            }

        suspend fun open() = when (type) {
            is ItemType.Folder ->
                file.toggleExpanded()
            is ItemType.File ->
                openFile(file.file)
        }
    }

    sealed class ItemType {
        class Folder(val isExpanded: Boolean, val canExpand: Boolean) : ItemType()
        class File(val ext: String) : ItemType()
    }

    private fun ExpandableFile.toItems(): List<Item> {
        fun ExpandableFile.addTo(list: MutableList<Item>) {
            list.add(Item(this))
            for (child in children) {
                child.addTo(list)
            }
        }

        val list = mutableListOf<Item>()
        addTo(list)
        return list
    }

}
