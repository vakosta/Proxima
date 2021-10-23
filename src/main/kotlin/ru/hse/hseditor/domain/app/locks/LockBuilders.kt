package ru.hse.hseditor.domain.app.locks

import kotlinx.coroutines.runBlocking
import ru.hse.hseditor.domain.app.lifetimes.Lifetime
import ru.hse.hseditor.domain.app.exceptions.ActionNotExecutedException

fun <T> runBlockingRead(block: () -> T, canExecuteBlock: (() -> Boolean)? = null): T {
    val readAction = object : ReadAction<T>() {
        var result: T? = null
        override fun execute() {
            result = block()
        }

        override fun canExecute() = if (canExecuteBlock != null) canExecuteBlock() else true
        override fun collectResult() = result ?: throw ActionNotExecutedException("Result is null!")
    }

    return runBlocking {
        readAction.actionContext = coroutineContext
        LocksGate.acquireReadActionLock(readAction)

        readAction.execute()
        readAction.collectResult()
    }
}

fun <T> runBlockingRead(block: () -> T) = runBlockingRead<T>(block, null)

fun runBlockingWrite(block: () -> Unit, canExecuteBlock: (() -> Boolean)? = null) {
    val writeAction = object : WriteAction() {
        override fun execute() = block()
        override fun canExecute() = if (canExecuteBlock != null) canExecuteBlock() else true
    }

    runBlocking {
        writeAction.actionContext = coroutineContext
        val interruptedActions = LocksGate.acquireWriteActionLock(writeAction)
        writeAction.execute()

        // Resume the interrupted actions
        for (action in interruptedActions) {
            action.lifetime.scopedLaunch { action.execute() }
        }
    }
}

fun runBlockingWrite(block: () -> Unit) {
    runBlockingWrite(block, null)
}

fun Lifetime.runBackgroundRead(block: () -> Unit, canExecuteBlock: (() -> Boolean)? = null) {
    val readAction = object : ReadAction<Unit>() {
        override fun execute() {
            block()
        }

        override fun canExecute() = if (canExecuteBlock != null) canExecuteBlock() else true
        override fun collectResult() = Unit
    }

    scopedLaunch {
        LocksGate.acquireReadActionLock(readAction)
        readAction.execute()
    }
}

fun Lifetime.runBackgroundRead(block: () -> Unit) = runBackgroundRead(block, null)

fun Lifetime.runBackgroundWrite(block: () -> Unit, canExecuteBlock: (() -> Boolean)? = null) {
    val writeAction = object : WriteAction() {
        override fun execute() = block()
        override fun canExecute() = if (canExecuteBlock != null) canExecuteBlock() else true
    }

    scopedLaunch {
        val interruptedActions = LocksGate.acquireWriteActionLock(writeAction)
        writeAction.execute()

        // Resume the interrupted actions
        for (action in interruptedActions) {
            action.lifetime.scopedLaunch { action.execute() }
        }
    }
}

// TODO, this mustn't be as excruciating
//fun runInterruptableAction()