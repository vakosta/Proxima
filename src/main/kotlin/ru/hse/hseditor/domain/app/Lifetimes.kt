package ru.hse.hseditor.domain.app

import kotlinx.coroutines.*

abstract class LifetimeDef(
    private val myLifetimeId: String,
    internal val parentLifetime: Lifetime? = null
) {
    init { parentLifetime?.alsoOnTerminate { terminateLifetime() } }

    abstract val lifetime: Lifetime

    fun terminateLifetime() = lifetime.terminate()

    val id get() = "DefinitionOf::$myLifetimeId"
}

abstract class Lifetime(
    val id: String,
    internal val scope: CoroutineScope
) {
    private val myOnTerminateList = mutableListOf<() -> Unit>()

    var myIsTerminated = false
    val isTerminated get() = myIsTerminated

    init {
        myOnTerminateList.add {
            if (scope.isActive) scope.cancel("Lifetime ended.")
        }
        myOnTerminateList.add {
            myIsTerminated = true
        }
    }

    fun alsoOnTerminate(block: () -> Unit) = myOnTerminateList.add(block)

    fun alsoBracket(begin: () -> Unit, end: () -> Unit) {
        begin()
        myOnTerminateList.add(end)
    }

    internal fun terminate() = myOnTerminateList.forEach { it() }

    fun scopedLaunch(block: suspend () -> Unit) = if (isTerminated) null else scope.launch { block() }

    fun <T> scopedAsync(block: suspend () -> T) = if (isTerminated) null else scope.async { block() }
}
