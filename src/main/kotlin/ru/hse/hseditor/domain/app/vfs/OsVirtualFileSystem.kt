package ru.hse.hseditor.domain.app.vfs

import ru.hse.hseditor.domain.app.lifetimes.Lifetime
import ru.hse.hseditor.domain.app.lifetimes.defineChildLifetime
import java.nio.file.*
import kotlin.concurrent.thread
import kotlin.io.path.isDirectory

// Modification tracker
// Autosaver ??
// DEBOUNCE !!!
// Save/read for individual files.
// Paging??? maybe later, i trust in the PieceTree

fun mountVFSAtPathLifetimed(
    lifetime: Lifetime,
    path: Path,
    linkOption: LinkOption = LinkOption.NOFOLLOW_LINKS
): OsVirtualFileSystem {
    require(path.isAbsolute) { "May only mount at an absolute path!" }
    require(path.isDirectory(linkOption)) { "Cannot mount a non-directory!" }

    // Java interop is ugly.

    val watcher = path.fileSystem.newWatchService()
    val watcherLifetime = defineChildLifetime(lifetime, "OsVirtualFileSystem::WatcherLifetime").lifetime
    watcherLifetime.alsoOnTerminate { watcher.close() }

    // TODO support interrupts??
    val visitor = MountVFSFileVisitor(watcher)
    Files.walkFileTree(path, visitor)
    if (visitor.exception != null) {
        throw visitor.exception!! // mamoy klyanus'!!
    }

    return OsVirtualFileSystem(lifetime, WatcherDescriptor(watcherLifetime, watcher), visitor.root)
}

internal class WatcherDescriptor(val lifetime: Lifetime, val watcher: WatchService) {
    // Wanted to do it within a coroutine, but couldn't figure out how.
    fun startWatching() {
        val th = thread(start = false, name = "File watcher thread", block = this::watch)
        lifetime.alsoOnTerminate { th.interrupt() }
        th.start()
    }

    private fun watch() {
        var isKeyValid = true
        lateinit var key: WatchKey

        while (!lifetime.isTerminated && isKeyValid) {
            key = try { watcher.take() } catch (e: InterruptedException) { return }

            key.pollEvents().forEach { processEvent(it) }

            isKeyValid = key.reset()
        }
    }

    private fun processEvent(event: WatchEvent<*>) = when (event.kind()) {
        StandardWatchEventKinds.ENTRY_CREATE -> {}
        StandardWatchEventKinds.ENTRY_DELETE -> {}
        StandardWatchEventKinds.ENTRY_MODIFY -> {}
        StandardWatchEventKinds.OVERFLOW -> {}
        else -> {}
    }
}

/**
 * Works only over the OS's file system. I still have little idea of how to write a proper VFS :/
 */
class OsVirtualFileSystem internal constructor(
    private val myLifetime: Lifetime,
    private val myWatcherDescriptor: WatcherDescriptor,
    val root: OsVirtualDirectory
) {

    init {
    }

    fun resolveFileAbsoluteOrNull(path: Path): OsVirtualFile? {
        require(path.isAbsolute) { "Path wasn't absolute!" }
        require(path.startsWith(root.path)) { "Path is outside the root of the VFS!" }

        return resolveFileRelativeOrNull(root.path.relativize(path))
    }

    fun resolveFileRelativeOrNull(path: Path) = resolveFileRelativeOrNullRec(root, path) as? OsVirtualFile
}

private fun resolveFileRelativeOrNullRec(osNode: OsVFSNode, path: Path): OsVFSNode? {
    val child = when (osNode) {
        is OsVirtualDirectory -> osNode.resolveChildNodeOrNull(path)
        is OsVirtualFile -> if (osNode.path == path) osNode else null
        is OsVirtualSymlink -> if (osNode.path == path) osNode else null
    } ?: return null

    return resolveFileRelativeOrNullRec(child, path)
}
