package ru.hse.hseditor.domain.common.locks

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO @thisisvolatile DOES NOT handle reetrancy :(((

internal object LocksGate {

    private var myOngoingWriteAction: WriteAction? = null
    private val myDispatchMutex = Mutex(false)

    private val myOngoingReadActionSet = mutableSetOf<ReadAction<*>>()

    /**
     * Acquires a read action lock, if there is no active write action.
     * Otherwise, suspends until its completion.
     */
    suspend fun <T> acquireReadActionLock(readAction: ReadAction<T>) {
        myDispatchMutex.withLock {
            myOngoingWriteAction?.join()
            myOngoingReadActionSet.add(readAction)
        }
    }

    suspend fun <T> letGoReadActionLock(readAction: ReadAction<T>) {
        myDispatchMutex.withLock {
            myOngoingReadActionSet.remove(readAction)
        }
    }

    /**
     * Acquires a write action lock once all read actions cease to execute and returns a list of
     * interrupted read actions that must be resumed after the write action is done.
     */
    suspend fun acquireWriteActionLock(writeAction: WriteAction): List<InterruptableReadAction> {
        val interruptedActions = mutableListOf<InterruptableReadAction>()

        myDispatchMutex.withLock {
            myOngoingWriteAction?.join()
            // Stop the world
            for (action in myOngoingReadActionSet) {
                if (action is InterruptableReadAction) {
                    action.interrupt()
                    interruptedActions.add(action)
                }
                action.join()
            }

            myOngoingWriteAction = writeAction
        }

        return interruptedActions
    }

    suspend fun letGoWriteActionLock(writeAction: WriteAction) {
        myDispatchMutex.withLock {
            myOngoingWriteAction = null
        }
    }
}
