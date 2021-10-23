package ru.hse.hseditor.domain.common.vfs

import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.lifetimes.defineChildLifetime
import java.nio.file.*
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.io.path.isDirectory

// Modification tracker
// Autosaver ??

fun mountVFSAtPathLifetimed(
    lifetime: Lifetime,
    absoluteRootDirPath: Path,
    linkOption: LinkOption = LinkOption.NOFOLLOW_LINKS
): OsVirtualFileSystem {
    require(absoluteRootDirPath.isAbsolute) { "May only mount at an absolute path!" }
    require(absoluteRootDirPath.isDirectory(linkOption)) { "Cannot mount a non-directory!" }

    // Java interop is ugly.

    val watcher = absoluteRootDirPath.fileSystem.newWatchService()
    val watcherLifetime = defineChildLifetime(lifetime, "OsVirtualFileSystem::WatcherLifetime").lifetime
    watcherLifetime.alsoOnTerminate { watcher.close() }

    val watcherDescriptor = WatcherDescriptor(watcherLifetime, watcher)
    val vfsStub = OsVirtualFileSystem(
        lifetime,
        watcherDescriptor,
        absoluteRootDirPath
    )

    // TODO support interrupts??
    val visitor = MountVFSFileVisitor(watcher, vfsStub)
    Files.walkFileTree(absoluteRootDirPath, visitor)
    if (visitor.exception != null) {
        throw visitor.exception!! // mamoy klyanus'!!
    }

    vfsStub.internalRoot = visitor.root
    watcherDescriptor.startWatching()
    // If we are here with no exceptions, it is no longer a stub!
    return vfsStub
}

internal class WatcherDescriptor(val lifetime: Lifetime, val watcher: WatchService) {
    // Wanted to do it within a coroutine, but couldn't figure out how.
    fun startWatching() {
        val th = thread(start = false, name = "File watcher thread", isDaemon = true, block = this::watch)
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

    private fun processEvent(event: WatchEvent<*>) {
        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
                val context = event.context()
                if (context !is Path) return

                LOG.info(context.toString())
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
                // Remove from vfs, signal update to frontend
            }
            StandardWatchEventKinds.ENTRY_MODIFY -> {
                // If not opened, do nothing, if opened, execute the events
            }
            StandardWatchEventKinds.OVERFLOW -> {
                // Log.warn
            }
            else -> {
                // Log.Warn
            }
        }
    }

    companion object {
        val LOG = Logger.getLogger(OsVirtualFileSystem::class.java.name)
    }
}

/**
 * Works only over the OS's file system. I still have little idea of how to write a proper VFS :/
 */
class OsVirtualFileSystem internal constructor(
    private val myLifetime: Lifetime,
    private val myWatcherDescriptor: WatcherDescriptor,
    val absoluteRootPath: Path
) {
    internal lateinit var internalRoot: OsVirtualDirectory
    val root get() = internalRoot

    fun resolveFileAbsoluteOrNull(path: Path): OsVirtualFile? {
        require(path.isAbsolute) { "Path wasn't absolute!" }
        require(path.startsWith(internalRoot.path)) { "Path is outside the root of the VFS!" }

        return resolveFileRelativeOrNull(internalRoot.path.relativize(path))
    }

    fun resolveFileRelativeOrNull(path: Path) = resolveFileRelativeOrNullRec(internalRoot, path) as? OsVirtualFile
}

private fun resolveFileRelativeOrNullRec(osNode: OsVFSNode, path: Path): OsVFSNode? {
    val child = when (osNode) {
        is OsVirtualDirectory -> osNode.resolveChildNodeOrNull(path)
        is OsVirtualFile -> if (osNode.path == path) osNode else null
        is OsVirtualSymlink -> if (osNode.path == path) osNode else null
    } ?: return null

    return resolveFileRelativeOrNullRec(child, path)
}
