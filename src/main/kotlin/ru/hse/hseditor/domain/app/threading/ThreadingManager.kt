package ru.hse.hseditor.domain.app.threading

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import java.util.*
import kotlin.coroutines.CoroutineContext

// TODO @thisisvolatile Set an upper bound for the amount of ReadActions?

data class ReadActionManagerEntry<T>(
    val deferred: Deferred<T>,
    val continuation: (T) -> Unit
)

data class ReadActionQueueEntry<T>(
    val readAction: ReadAction<T>,
    val continuation: (Any?) -> Unit
)

data class ReadActionAwaitResult<T>(
    val readAction: ReadAction<Any?>,
    val result: Any?,
    val continuation: (Any?) -> Unit
)

// TODO @thisisvolatile Continuation should be optional

class ThreadingManager internal constructor(
    private val writeDispatcher: CoroutineDispatcher,
    private val readDispatcher: CoroutineDispatcher,
    // TODO @thisisvolatile Theoretically, coro scopes should be tied to owner lifetimes.
    // I'll make it this way, if we have enough time.
    private val globalCoroutineScope: CoroutineScope
) {

    private var activeWriteAction: WriteAction? = null

    // TODO @thisisvolatile rewrite with generic variance (?)
    private val activeReadActions = mutableMapOf<ReadAction<Any?>, ReadActionManagerEntry<Any?>>()

    private val writeActionQueue: Queue<WriteAction> = LinkedList()
    private val readActionQueue: Queue<ReadActionQueueEntry<Any?>> = LinkedList()

    internal suspend fun flushReadActionsQueue(flushContext: CoroutineContext) {
        while (readActionQueue.isNotEmpty()) {
            val (readAction, continuation) = readActionQueue.remove()
            dispatchRead(readAction, continuation)
        }

        awaitAllReadActionsInContext(flushContext)
    }

    internal suspend fun awaitAllReadActionsInContext(context: CoroutineContext) {
        while (context.isActive && activeReadActions.isNotEmpty()) {
            val (completedAction, actionValue, continuation) = select<ReadActionAwaitResult<Any?>> {
                activeReadActions.forEach { (action, entry) ->
                    entry.deferred.onAwait {
                        ReadActionAwaitResult(action, it, entry.continuation)
                    }
                }
            }
            activeReadActions.remove(completedAction)

            // Is executed in the same scope that this function is called in.
            continuation(actionValue)
        }
    }

    /**
     * Read actions will occur only after the write actions.
     */
    internal fun <T> dispatchRead(readAction: ReadAction<T>, continuation: (Any?) -> Unit) {
        val deferred = globalCoroutineScope.async {
            readAction.startExecuteInContextForResult(coroutineContext)
        }
        activeReadActions[readAction] = ReadActionManagerEntry(deferred, continuation)

        globalCoroutineScope.launch { awaitAllReadActionsInContext(readAction.readActionContext) }
    }

    /**
     * The write actions will interrupt all [InterruptableReadAction]s
     * and wait impatiently for all the rest [ReadAction]s to finish.
     */
    internal fun dispatchWrite() {
        if (activeReadActions.isNotEmpty()) {
            // Interrupt and join all
        }
        // Do write action
        // Go back to doing read actions
    }
}

interface WriteAction

class BasicWriteAction internal constructor() : WriteAction {

}

abstract class ReadAction<out TResult> internal constructor(
    protected val myDispatcher: CoroutineDispatcher,
    protected val myThreading: ThreadingManager
) {

    internal lateinit var readActionContext: CoroutineContext

    open suspend fun startExecuteInContextForResult(context: CoroutineContext): TResult? {
        readActionContext = context
        return withContext(myDispatcher) {
            execute()
        }
    }

    abstract suspend fun execute(): TResult?

}

abstract class InterruptableReadAction<out TResult, TIntermediateResult> internal constructor(
    dispatcher: CoroutineDispatcher,
    threading: ThreadingManager
) : ReadAction<TResult>(dispatcher, threading) {

    /**
     * Starts or continues this [InterruptableReadAction].
     */
    override suspend fun startExecuteInContextForResult(context: CoroutineContext): TResult? {
        readActionContext = context
        if (!calculateIsDone() && calculateIsValid()) {
            return execute()
        }
        return null
    }

    private val myIntermediateResults = mutableListOf<TIntermediateResult>()

    /**
     * Overridden to call [doExecuteStep] while [myIsInterrupted] is false.
     */
    override suspend fun execute(): TResult? {
        while (!calculateIsDone()) {
            myIntermediateResults.add(doExecuteStep())

            if (!readActionContext.isActive) {
                return null
            }
        }
        return collectResult(myIntermediateResults)
    }


    abstract suspend fun doExecuteStep(): TIntermediateResult

    abstract fun collectResult(result: List<TIntermediateResult>): TResult

    abstract fun calculateIsDone(): Boolean

    abstract fun calculateIsValid(): Boolean
}

fun queueReadAction(block: () -> Unit) {

}

fun queueWriteAction(block: () -> Unit) {

}