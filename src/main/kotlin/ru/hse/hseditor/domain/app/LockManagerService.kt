package ru.hse.hseditor.domain.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext


// TODO @thisisvolatile Continuation should be optional

class LockManagerService internal constructor(
    private val writeDispatcher: CoroutineDispatcher,
    private val readDispatcher: CoroutineDispatcher,
) {

    // Maps must be concurrent!
    private val myActiveReadActions = mutableMapOf<ReadAction, Job>()
    private var myActiveWriteAction: Pair<WriteAction, Job>? = null

    private val myInterruptedReadActions = mutableMapOf<ReadAction, Job>()
    // End concurrent stuff

    /**
     * [ReadAction]s will occur only after the [WriteAction]s.
     */
    internal fun <T> dispatchRead(readAction: ReadAction) {

    }

    /**
     * The [WriteAction]s will interrupt all [InterruptableReadAction]s
     * and wait impatiently for all the rest [ReadAction]s to finish.
     */
    internal fun dispatchWrite(writeAction: WriteAction) {

    }

    private fun cancelAllReadActions() {

    }

    internal fun notifyReadDone() { }

    internal fun notifyWriteDone() { }
}

abstract class WriteAction internal constructor() {

    open suspend fun startExecute() {
        execute()
    }

    abstract suspend fun execute()
}

abstract class ReadAction internal constructor() {

    internal lateinit var readActionContext: CoroutineContext

    open suspend fun startOrContinueExecute() {
        execute()
    }

    abstract suspend fun execute()

}

abstract class InterruptableReadAction internal constructor() : ReadAction() {

    /**
     * Starts or continues this [InterruptableReadAction].
     */
    override suspend fun startOrContinueExecute() {
        if (calculateIsValid()) {
            execute()
        }
    }

    /**
     * Overridden to call [doExecuteStep], while
     */
    final override suspend fun execute() {
        while (!calculateIsDone()) {
            doExecuteStep()
            if (!readActionContext.isActive) return
        }

        finish()
    }


    abstract suspend fun doExecuteStep()

    abstract suspend fun finish()

    abstract fun calculateIsDone(): Boolean

    abstract fun calculateIsValid(): Boolean
}