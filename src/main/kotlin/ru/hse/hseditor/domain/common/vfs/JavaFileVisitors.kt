package ru.hse.hseditor.domain.common.vfs

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Logger

internal class MountVFSFileVisitor(
    private val myWatcher: WatchService,
    private val myVsfStub: OsVirtualFileSystem
) : FileVisitor<Path> {
    private var myRoot: OsVirtualDirectory? = null
    val root: OsVirtualDirectory get() = myRoot ?: throw IllegalStateException("Walk was not started!")

    private var myException: IOException? = null
    val exception: IOException? get() = myException

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        require(dir != null) { "Null file encountered while mounting a directory!" }
        require(attrs != null) { "Null file attributes encountered while mounting a directory!" }

        if (myRoot != null) {
            val subDirectoryVisitor = MountVFSFileVisitor(myWatcher, myVsfStub)
            Files.walkFileTree(dir, subDirectoryVisitor)
            root.mutChildren.add(subDirectoryVisitor.root)

            return FileVisitResult.SKIP_SUBTREE
        }

        myRoot = OsVirtualDirectory(myVsfStub.fsLifetime, null, dir, myVsfStub)
        dir.register(
            myWatcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        require(file != null) { "Null file encountered while mounting a directory!" }
        require(attrs != null) { "Null file attributes encountered while mounting a directory!" }

        if (attrs.isSymbolicLink) {
            root.mutChildren.add(OsVirtualSymlink(myVsfStub.fsLifetime, root, file, myVsfStub))
        } else if (attrs.isRegularFile) {
            root.mutChildren.add(OsVirtualFile(myVsfStub.fsLifetime, root, file, myVsfStub))
        } else {
            LOG.warning("Found a leaf that is neither symlink nor file.")
        }

        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
        require(file != null) { "Null file encountered while mounting a directory!" }
        require(exc != null) { "Null exception encountered while mounting a directory!" }

        myException = exc

        return FileVisitResult.TERMINATE
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        if (exc != null) {
            myException = exc
            return FileVisitResult.TERMINATE
        }

        return FileVisitResult.CONTINUE
    }

    companion object {
        val LOG = Logger.getLogger(MountVFSFileVisitor::class.java.name)
    }
}