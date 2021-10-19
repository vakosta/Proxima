package ru.hse.hseditor.domain.app.vfs

import java.nio.file.Path

private val listEmpty = listOf<VFSNode>()

sealed interface VFSNode {
    val parent: VFSNode?
    val children: List<VFSNode>
}

sealed interface OsVFSNode : VFSNode {
    val path: Path
}

class OsVirtualFile(
    override val parent: OsVFSNode?,
    override val path: Path
) : OsVFSNode {
    override val children get() = listEmpty


}

class OsVirtualDirectory(
    override val parent: OsVFSNode?,
    override val path: Path
): OsVFSNode {
    internal val mutChildren = mutableListOf<OsVFSNode>()
    override val children: List<OsVFSNode> get() = mutChildren

    fun resolveChildNodeOrNull(path: Path) = children.firstOrNull { path.startsWith(it.path) }
}

class OsVirtualSymlink(
    override val parent: OsVFSNode?,
    override val path: Path
) : OsVFSNode {
    override val children get() = listEmpty
}
