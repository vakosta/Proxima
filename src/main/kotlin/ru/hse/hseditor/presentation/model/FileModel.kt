package ru.hse.hseditor.presentation.model

import ru.hse.hseditor.domain.common.vfs.OsVFSNode
import ru.hse.hseditor.domain.common.vfs.OsVirtualDirectory
import ru.hse.hseditor.domain.common.vfs.VFSNode
import kotlin.io.path.name

fun OsVFSNode.toFileModel(): FileModel = FileModel(
    path.name,
    this is OsVirtualDirectory,
    (this as? OsVirtualDirectory)?.children?.map { it.toFileModel() } ?: listOf(),
    this is OsVirtualDirectory,
    this
)

class FileModel(
    val name: String,
    val isDirectory: Boolean,
    val children: List<FileModel>,
    val hasChildren: Boolean,
    val vfsNode: VFSNode?
)
