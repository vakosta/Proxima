package ru.hse.hseditor.presentation.model

import ru.hse.hseditor.domain.common.ChangeKind
import ru.hse.hseditor.domain.common.ObservableList
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.vfs.*
import ru.hse.hseditor.domain.highlights.ExtModificationDesc
import ru.hse.hseditor.domain.highlights.ExtModificationKind
import ru.hse.hseditor.domain.text.document.DocumentSource
import kotlin.io.path.name

private fun FileModel.adviseDirectoryEvents(lifetime: Lifetime, dir: OsVirtualDirectory) {
    dir.mutChildren.addRemove.advise(lifetime) { event ->
        when (event.kind) {
            ChangeKind.ADD -> children.addFiring(event.it.toFileModelLifetimed(lifetime))
            ChangeKind.REMOVE -> {
                val childFileModel = children.firstOrNull { it.vfsNode == event.it }
                require(childFileModel != null) { "Must be a child! (event dispatch is broken)" }
                val documentSource = childFileModel.vfsNode as? DocumentSource

                documentSource?.openedDocuments?.forEach {
                    it.textState.externalModificationEvent.fire(
                        ExtModificationDesc(ExtModificationKind.DOCUMENT_DELETED)
                    )
                }

                children.removeFiring(childFileModel)
            }
        }
    }

    dir.changedEvent.advise(lifetime) {
        name = dir.path.name
    }
}

private fun FileModel.adviseFileEvents(lifetime: Lifetime, file: OsVirtualFile) {
    file.changedEvent.advise(lifetime) {
        file.openedDocuments.forEach { it.textState.externalModificationEvent.fire(
            ExtModificationDesc(ExtModificationKind.DOCUMENT_DISC_SYNC)
        ) }
    }
}

private fun FileModel.adviseSymlinkEvents(lifetime: Lifetime, symlink: OsVirtualSymlink) {
    symlink.changedEvent.advise(lifetime) {
        this.name = symlink.path.fileName.name
    }
}

fun OsVFSNode.toFileModelLifetimed(lifetime: Lifetime): FileModel = FileModel(
    path.name,
    this is OsVirtualDirectory,
    (this as? OsVirtualDirectory)?.children
        ?.map { it.toFileModelLifetimed(lifetime) }
        ?.toMutableList()?.let { ObservableList(it) }
        ?: ObservableList(),
    this is OsVirtualDirectory,
    this
).apply {
    when (val osVFSNode = vfsNode as? OsVFSNode) {
        is OsVirtualDirectory -> adviseDirectoryEvents(lifetime, osVFSNode)
        is OsVirtualFile -> adviseFileEvents(lifetime, osVFSNode)
        is OsVirtualSymlink -> adviseSymlinkEvents(lifetime, osVFSNode)
    }
}

class FileModel(
    var name: String,
    val isDirectory: Boolean,
    val children: ObservableList<FileModel>,
    val hasChildren: Boolean,
    val vfsNode: VFSNode?
)
