package ru.hse.hseditor.domain.common.locks

import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import ru.hse.hseditor.domain.common.Event
import ru.hse.hseditor.domain.common.lifetimes.Lifetime
import kotlin.coroutines.CoroutineContext

abstract class CoroutineAction internal constructor() {
    lateinit var actionContext: CoroutineContext

    abstract fun execute()

    abstract fun canExecute(): Boolean

    suspend fun join() = actionContext.job.join()
}

interface InterruptableAction {
    var actionContext: CoroutineContext
    fun interrupt() = actionContext.job.cancel()
}

abstract class WriteAction internal constructor() : CoroutineAction()

abstract class ReadAction<T> internal constructor() : CoroutineAction() {

    abstract fun collectResult(): T
}

abstract class InterruptableReadAction internal constructor(
    internal val lifetime: Lifetime
) : ReadAction<Unit>(), InterruptableAction {

    /**
     * Overridden to call [doExecuteStep], while can execute.
     */
    final override fun execute() {
        while (canExecute() && !calculateIsDone() && !lifetime.isTerminated) {
            doExecuteStep()
            if (!actionContext.isActive) return
        }

        finish()
    }

    final override fun collectResult() { }

    abstract fun doExecuteStep()

    abstract fun finish()

    abstract fun calculateIsDone(): Boolean
}