package ru.hse.hseditor.domain.common.lifetimes

import kotlinx.coroutines.*

private val appBackgroundDispatcher = Dispatchers.Default

fun defineLifetime(id: String) =
    LifetimeDef(id, Lifetime(id, CoroutineScope(appBackgroundDispatcher + SupervisorJob())))

fun defineChildLifetime(parentLifetime: Lifetime, id: String) =
    LifetimeDef(id, Lifetime(id, CoroutineScope(appBackgroundDispatcher + SupervisorJob())), parentLifetime)

class LifetimeDef internal constructor(
    private val myLifetimeId: String,
    val lifetime: Lifetime,
    internal val parentLifetime: Lifetime? = null
) {
    init { parentLifetime?.alsoOnTerminate { terminateLifetime() } }

    fun terminateLifetime() = lifetime.terminate()

    val id get() = "DefinitionOf::$myLifetimeId"
}

class Lifetime internal constructor(
    val id: String,
    private val scope: CoroutineScope
) {
    private val myOnTerminateList = mutableListOf<() -> Unit>()

    var myIsTerminated = false
    val isTerminated get() = myIsTerminated

    init {
        myOnTerminateList.add {
            if (scope.isActive) scope.cancel("Lifetime ended.")
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
