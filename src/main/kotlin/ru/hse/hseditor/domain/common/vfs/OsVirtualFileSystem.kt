package ru.hse.hseditor.domain.common.vfs

import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import ru.hse.hseditor.domain.common.lifetimes.defineChildLifetime
import ru.hse.hseditor.domain.common.locks.runBlockingWrite
import java.io.IOException
import java.nio.file.*
import java.time.Instant
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.io.path.*

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
            try {
                key = watcher.take()
            } catch (e: Exception) {
                LOG.info(e.message)
            }


            val compressedEvents = compressEvents(key.watchable(), key.pollEvents())
            compressedEvents.forEach { processEvent(key.watchable(), it) }

            isKeyValid = key.reset()
        }
    }

    /**
     * This depends on Java returning events in order.
     * It does on Unix systems.
     */
    private fun compressEvents(dir: Watchable, events: List<WatchEvent<*>>): Iterable<WatchEvent<*>> {
        if (dir !is Path) return emptyList()

        val lastSeenEvents = mutableMapOf<Path, WatchEvent<*>>()
        for (event in events) {
            val context = event.context()
            if (context !is Path) continue

            val resolvedContext = dir.resolve(context)
            val lastEvent = lastSeenEvents[resolvedContext]

            // First event for this path
            if (lastEvent == null) {
                lastSeenEvents[resolvedContext] = event
                continue
            }

            // If the last event was REMOVE and a new one is UPDATE, skip it.
            if (
                lastEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE
                && event.kind() != StandardWatchEventKinds.ENTRY_DELETE
            ) {
                lastSeenEvents[resolvedContext] = event
                continue
            }

            // Replace create with update.
            // (because update creates a file)
            if (lastEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE
                && event.kind() == StandardWatchEventKinds.ENTRY_MODIFY
            ) {
                lastSeenEvents[resolvedContext] = event
                continue
            }

            // Remove the entry, if we encountered a delete.
            if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                lastSeenEvents.remove(resolvedContext)
                continue
            }

            // If the last event matches with a new one, skip it.
            if (lastEvent.kind() == event.kind()) {
                lastSeenEvents[resolvedContext] = event
                continue
            }
        }

        return lastSeenEvents.values
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

        val resolvedContext = dir.resolve(context)

        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> handleEntryCreate(dir, resolvedContext)
            StandardWatchEventKinds.ENTRY_DELETE -> handleEntryDelete(dir, resolvedContext)
            StandardWatchEventKinds.ENTRY_MODIFY -> {
                // Modification timestamps are here because it's java
                // and, although it declares to be WORA, it's REALLY dependant
                // onto the underlying system APIs. And some FSes are bad with clumping
                // modifications into a SINGLE event. And some don't support modification timestamps!
                try {
                    val modifiedInstant = resolvedContext.getLastModifiedTime().toInstant()
                    if (modifiedInstant == Instant.EPOCH) {
                        LOG.warning("Couldn't get the modification time!")
                        handleEntryModify(dir, resolvedContext)
                    } else if (modifiedInstant != myLastEntryModifyTimestamp) {
                        handleEntryModify(dir, resolvedContext)
                    }
                    myLastEntryModifyTimestamp = resolvedContext.getLastModifiedTime().toInstant()
                } catch (e: IOException) {
                    LOG.warning("[CREATE]: Can't touch timestamp: ${e.message}")
                }
            }
            StandardWatchEventKinds.OVERFLOW -> LOG.warning("Some FS events were ignored due to an overflow")
            else -> LOG.warning("Some user event was ignored!")
        }
    }

    private fun handleEntryCreate(dir: Path, resolvedContext: Path) {
        require(resolvedContext.isAbsolute) { "Context must be resolved!" }

        if (!resolvedContext.isDirectory()
            && !resolvedContext.isRegularFile()
            && !resolvedContext.isSymbolicLink()
        ) return

        observedVfs.makeNodeFromPath(dir, resolvedContext)
    }

    private fun handleEntryDelete(dir: Path, resolvedContext: Path) {
        require(resolvedContext.isAbsolute) { "Context must be resolved!" }
        observedVfs.removeNode(dir, resolvedContext)
    }

    private fun handleEntryModify(dir: Path, resolvedContext: Path) {
        require(resolvedContext.isAbsolute) { "Context must be resolved!" }

        observedVfs.notifyNodeChanged(dir, resolvedContext)
    }

    companion object {
        val LOG = Logger.getLogger(OsVirtualFileSystem::class.java.name)
    }
}

/**
 * Works only over the OS's file system. I still have little idea of how to write a proper VFS :/
 */
class OsVirtualFileSystem internal constructor(
    val fsLifetime: Lifetime,
    val absoluteRootPath: Path
) {
    companion object {
        val LOG = Logger.getLogger(OsVirtualFileSystem::class.java.name)
    }

    internal lateinit var internalRoot: OsVirtualDirectory
    val root get() = internalRoot

    fun resolveNodeAbsoluteOrNull(path: Path): OsVFSNode? {
        require(path.isAbsolute) { "Path wasn't absolute!" }
        require(path.startsWith(internalRoot.path)) { "Path is outside the root of the VFS!" }

        return resolveFileAbsoluteOrNullRec(root, path)
    }

    fun resolveFileAbsoluteOrNull(path: Path) = resolveNodeAbsoluteOrNull(path) as? OsVirtualFile
    fun resolveDirAbsoluteOrNull(path: Path) = resolveNodeAbsoluteOrNull(path) as? OsVirtualDirectory

    fun makeDirectory(absoluteAt: Path, absoluteDir: Path): OsVFSNode {
        require(absoluteAt.isAbsolute && absoluteDir.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }

        val directory = OsVirtualDirectory(fsLifetime, parentDir, absoluteDir, this)
        parentDir.tryAddChildFiring(directory)
        return directory
    }

    fun makeFile(absoluteAt: Path, absoluteFile: Path): OsVFSNode {
        require(absoluteAt.isAbsolute && absoluteFile.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }

        val virtualFile = OsVirtualFile(fsLifetime, parentDir, absoluteFile, this)
        parentDir.tryAddChildFiring(virtualFile)
        return virtualFile
    }

    fun makeSymlink(absoluteAt: Path, absoluteSymlink: Path): OsVFSNode {
        require(absoluteAt.isAbsolute && absoluteSymlink.isAbsolute) {
            "Some path is not absolute!"
        }

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        require(parentDir != null) { "Parent node was not a directory!" }

        val symlink = OsVirtualSymlink(fsLifetime, parentDir, absoluteSymlink, this)
        parentDir.tryAddChildFiring(symlink)
        return symlink
    }

    fun makeNodeFromPath(absoluteAt: Path, absoluteChild: Path): OsVFSNode? {
        require(absoluteAt.isAbsolute && absoluteChild.isAbsolute) {
            "Some path is not absolute!"
        }

        return if (absoluteChild.isDirectory()) {
            makeDirectory(absoluteAt, absoluteChild)
        } else if (absoluteChild.isRegularFile()) {
            makeFile(absoluteAt, absoluteChild)
        } else if (absoluteChild.isSymbolicLink()) {
            makeSymlink(absoluteAt, absoluteChild)
        } else {
            LOG.warning("[CREATE] Can't make sense of $absoluteChild")
            null
        }
    }

    fun removeNode(absoluteAt: Path, absoluteChild: Path) {
        require(absoluteAt.isAbsolute && absoluteChild.isAbsolute) {
            "Some path is not absolute!"
        }

        LOG.info("[REMOVE] $absoluteChild")

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        val existingChild = parentDir?.resolveChildNodeOrNull(absoluteChild)

        if (existingChild == null) {
            LOG.warning("[REMOVE] Not yet created!")
            return
        }

        parentDir.removeChildFiring(existingChild)
    }

    fun notifyNodeChanged(absoluteAt: Path, absoluteChild: Path) {
        require(absoluteAt.isAbsolute && absoluteChild.isAbsolute) {
            "Some path is not absolute!"
        }

        LOG.info("[UPDATE] $absoluteChild")

        val parentDir = resolveDirAbsoluteOrNull(absoluteAt)
        val existingChild = parentDir?.resolveChildNodeOrNull(absoluteChild)

        if (existingChild == null) {
            LOG.warning("[UPDATE] Not yet created! Creating...")
            makeNodeFromPath(absoluteAt, absoluteChild)
            return
        }

        existingChild.changedEvent.fire(Unit)
    }
}

private fun resolveFileAbsoluteOrNullRec(osNode: OsVFSNode?, path: Path): OsVFSNode? = when (osNode) {
    is OsVirtualDirectory -> if (osNode.path == path) osNode else resolveFileAbsoluteOrNullRec(
        osNode.resolveChildNodeOrNull(path),
        path
    )
    is OsVirtualFile -> if (osNode.path == path) osNode else null
    is OsVirtualSymlink -> if (osNode.path == path) osNode else null
    null -> null
}
