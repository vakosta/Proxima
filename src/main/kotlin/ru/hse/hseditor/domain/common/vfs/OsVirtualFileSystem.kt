package ru.hse.hseditor.domain.common.vfs

import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.lifetimes.defineChildLifetime
import ru.hse.hseditor.domain.common.locks.runBlockingWrite
import java.nio.file.*
import java.time.Instant
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

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

    val vfsStub = OsVirtualFileSystem(
        lifetime,
        absoluteRootDirPath
    )

    val watcher = absoluteRootDirPath.fileSystem.newWatchService()
    val watcherLifetime = defineChildLifetime(lifetime, "OsVirtualFileSystem::WatcherLifetime").lifetime
    watcherLifetime.alsoOnTerminate { watcher.close() }

    val watcherDescriptor = WatcherDescriptor(watcherLifetime, watcher, vfsStub)

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

internal class WatcherDescriptor(
    val lifetime: Lifetime,
    val watcher: WatchService,
    val observedVfs: OsVirtualFileSystem
) {

    private var myLastEntryModifyTimestamp = Instant.now()

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
            key = try {
                watcher.take()
            } catch (e: Exception) {
                return
            }

            key.pollEvents().forEach { processEvent(key.watchable(), it) }

            isKeyValid = key.reset()
        }
    }

    private fun processEvent(dir: Watchable, event: WatchEvent<*>) {
        if (dir !is Path) {
            LOG.warning("Got a watchable that is not a path!")
            return
        }

        val context = event.context()
        if (context !is Path) {
            LOG.warning("Got an event context that is not a path!")
            return
        }

        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> handleEntryCreate(dir, context)
            StandardWatchEventKinds.ENTRY_DELETE -> handleEntryDelete(dir, context)
            StandardWatchEventKinds.ENTRY_MODIFY -> {
                // Modification timestamps are here because it's java
                // and, although it declares to be WORA, it's REALLY dependant
                // onto the underlying system APIs. And some FSes are bad with clumping
                // modifications into a SINGLE event. And some don't support modification timestamps!
                val modifiedInstant = context.getLastModifiedTime().toInstant()
                if (modifiedInstant == Instant.EPOCH) {
                    LOG.warning("System's FS does not support file modification timestamps!")
                    handleNodeModified(dir, context)
                } else if (modifiedInstant != myLastEntryModifyTimestamp) {
                    handleNodeModified(dir, context)
                    myLastEntryModifyTimestamp = context.getLastModifiedTime().toInstant()
                }
            }
            StandardWatchEventKinds.OVERFLOW -> LOG.warning("Some FS events were ignored due to an overflow")
            else -> LOG.warning("Some user event was ignored!")
        }
    }

    private fun handleEntryCreate(dir: Path, context: Path) = runBlockingWrite {
        if (context.isDirectory()) {
            observedVfs.makeDirectory(dir, dir.resolve(context))
        } else if (context.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
            observedVfs.makeFile(dir, dir.resolve(context))
        } else {
            LOG.warning("Some unknown thing was created!")
        }
    }

    private fun handleEntryDelete(dir: Path, context: Path) = runBlockingWrite {
        observedVfs.removeNode(dir, dir.resolve(context))
    }

    private fun handleNodeModified(dir: Path, context: Path) {
        observedVfs.notifyNodeChanged(dir, dir.resolve(context))
    }

    companion object {
        val LOG = Logger.getLogger(OsVirtualFileSystem::class.java.name)
    }
}

enum class NodeChangeKind { ADD, REMOVE }

data class NodeChangingDescriptor(val kind: NodeChangeKind, val node: OsVFSNode)

/**
 * Works only over the OS's file system. I still have little idea of how to write a proper VFS :/
 */
class OsVirtualFileSystem internal constructor(
    val fsLifetime: Lifetime,
    val absoluteRootPath: Path
) {
    internal lateinit var internalRoot: OsVirtualDirectory
    val root get() = internalRoot

    fun resolveNodeAbsoluteOrNull(path: Path): OsVFSNode? {
        require(path.isAbsolute) { "Path wasn't absolute!" }
        require(path.startsWith(internalRoot.path)) { "Path is outside the root of the VFS!" }

        return resolveFileAbsoluteOrNullRec(root, path)
    }

    fun resolveFileAbsoluteOrNull(path: Path) = resolveNodeAbsoluteOrNull(path) as? OsVirtualFile
    fun resolveDirAbsoluteOrNull(path: Path) = resolveNodeAbsoluteOrNull(path) as? OsVirtualDirectory

    fun makeDirectory(absoluteAt: Path, absoluteDir: Path) {
        require(absoluteAt.isAbsolute && absoluteDir.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }

        parentDir.addChildFiring(OsVirtualFile(fsLifetime, parentDir, absoluteDir, this))
    }

    fun makeFile(absoluteAt: Path, absoluteFile: Path) {
        require(absoluteAt.isAbsolute && absoluteFile.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }

        parentDir.addChildFiring(OsVirtualFile(fsLifetime, parentDir, absoluteFile, this))
    }

    fun removeNode(absoluteAt: Path, absoluteChild: Path) {
        require(absoluteAt.isAbsolute && absoluteChild.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }
        val existingChild = parentDir.resolveChildNodeOrNull(absoluteChild)
        require(existingChild != null) { "Child to delete does not exist!" }

        parentDir.removeChildFiring(existingChild)
    }

    fun notifyNodeChanged(absoluteAt: Path, absoluteChild: Path) {
        require(absoluteAt.isAbsolute && absoluteChild.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }
        val existingChild = parentDir.resolveChildNodeOrNull(absoluteChild)
        require(existingChild != null) { "Child to delete does not exist!" }

        existingChild.changedEvent.fire(Unit)
    }
}

private fun resolveFileAbsoluteOrNullRec(osNode: OsVFSNode, path: Path): OsVFSNode? {
    val child = when (osNode) {
        is OsVirtualDirectory -> osNode.resolveChildNodeOrNull(path)
        is OsVirtualFile -> if (osNode.path == path) osNode else null
        is OsVirtualSymlink -> if (osNode.path == path) osNode else null
    } ?: return null

    return resolveFileAbsoluteOrNullRec(child, path)
}
