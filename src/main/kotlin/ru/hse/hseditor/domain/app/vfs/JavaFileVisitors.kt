package ru.hse.hseditor.domain.app.vfs

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.Stack

internal class MountVFSFileVisitor(
    private val myWatcher: WatchService,
    private val myVsfStub: OsVirtualFileSystem
) : FileVisitor<Path> {
    private val myDirectoryStack = Stack<OsVirtualDirectory>()

    private var myRoot: OsVirtualDirectory? = null
    val root: OsVirtualDirectory get() = myRoot ?: throw IllegalStateException("Walk was not started!")

    private var myException: IOException? = null
    val exception: IOException? get() = myException

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        require(dir != null) { "Null file encountered while mounting a directory!" }
        require(attrs != null) { "Null file attributes encountered while mounting a directory!" }

        lateinit var vDir: OsVirtualDirectory
        if (myRoot == null) {
            vDir = OsVirtualDirectory(myDirectoryStack.firstOrNull(), dir.relativize(dir), myVsfStub)
            myRoot = vDir
        } else {
            vDir = OsVirtualDirectory(myDirectoryStack.first(), root.path.relativize(dir), myVsfStub)
        }

        myDirectoryStack.push(vDir)

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
            myDirectoryStack.first().mutChildren.add(
                OsVirtualSymlink(
                    myDirectoryStack.first(),
                    root.path.relativize(file),
                    myVsfStub
                )
            )
        } else if (attrs.isRegularFile) {
            myDirectoryStack.first().mutChildren.add(
                OsVirtualFile(
                    myDirectoryStack.first(),
                    root.path.relativize(file),
                    myVsfStub
                )
            )
        } else {
            // Warn?
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

        myDirectoryStack.pop()

        return FileVisitResult.CONTINUE
    }
}
