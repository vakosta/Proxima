package ru.hse.hseditor.presentation.model

import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.vfs.*
import ru.hse.hseditor.domain.highlights.ExternalModificationDescriptor
import ru.hse.hseditor.domain.highlights.ExternalModificationKind
import ru.hse.hseditor.domain.text.document.Document
import ru.hse.hseditor.domain.text.document.DocumentSource
import ru.hse.hseditor.domain.text.document.handleChangesFromSource
import kotlin.io.path.name

private fun FileModel.adviseDirectoryEvents(lifetime: Lifetime, dir: OsVirtualDirectory) {
    dir.beforeChildAddRemoveEvent.advise(lifetime) { desc ->
        when (desc.kind) {
            NodeChangeKind.ADD -> children.add(desc.node.toFileModelLifetimed(lifetime))
            NodeChangeKind.REMOVE -> {
                val childFileModel = children.firstOrNull { it.vfsNode == desc.node }
                require(childFileModel != null) { "Must be a child! (event dispatch is broken)" }
                val documentSource = childFileModel.vfsNode as? DocumentSource

                documentSource?.openedDocuments?.forEach {
                    it.textState.externalModificationEvent.fire(
                        ExternalModificationDescriptor(ExternalModificationKind.DELETED_FROM_DISC)
                    )
                }

                children.remove()
            }
        }
    }
    dir.changedEvent.advise(lifetime) {
        name = dir.path.name
    }
}

private fun FileModel.adviseFileEvents(lifetime: Lifetime, file: OsVirtualFile) {
    file.changedEvent.advise(lifetime) {
        file.openedDocuments.forEach {

        }
    }
}

fun OsVFSNode.toFileModelLifetimed(lifetime: Lifetime): FileModel = FileModel(
    path.name,
    this is OsVirtualDirectory,
    (this as? OsVirtualDirectory)?.children?.map { it.toFileModelLifetimed(lifetime) }?.toMutableList()
        ?: mutableListOf(),
    this is OsVirtualDirectory,
    this
).apply {
    val osVFSNode = vfsNode as? OsVFSNode
    when (osVFSNode) {
        is OsVirtualDirectory -> adviseDirectoryEvents(lifetime, osVFSNode)
        is OsVirtualFile -> adviseFileEvents(lifetime, osVFSNode)
        is OsVirtualSymlink -> TODO()
        null -> TODO()
    }
}

class FileModel(
    var name: String,
    val isDirectory: Boolean,
    val children: MutableList<FileModel>,
    val hasChildren: Boolean,
    val vfsNode: VFSNode?
)
