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

    fun alsoOnTerminate(block: () -> Unit) = myOnTerminateList.add(block)

    fun alsoBracket(begin: () -> Unit, end: () -> Unit) {
        begin()
        myOnTerminateList.add(end)
    }

    internal fun terminate() = myOnTerminateList.forEach { it() }
}

class UiLifetimeDef(
    lifetimeId: String, parentLifetime: UiLifetime? = null
) : LifetimeDef(lifetimeId, parentLifetime) {
    override val lifetime = UiLifetime(
        lifetimeId,
        parentLifetime?.scope ?: MainScope()
    )
}

class UiLifetime(
    id: String,
    scope: CoroutineScope
) : Lifetime(id, scope) {
    fun launchUiAction(block: () -> Unit) = scope.launch { block() }
    fun <T> asyncUiAction(block: () -> T) = scope.async { block() }
}

class BackgroundLifetimeDef(
    lifetimeId: String,
    parentLifetime: UiLifetime? = null
) : LifetimeDef(lifetimeId, parentLifetime) {
    override val lifetime = BackgroundLifetime(
        lifetimeId,
        parentLifetime?.scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )
}

class BackgroundLifetime(
    id: String,
    scope: CoroutineScope
) : Lifetime(id, scope) {
    fun launchBackgroundAction(block: () -> Unit) = scope.launch { block() }
    fun <T> asyncBackgroundAction(block: () -> T) = scope.launch { block() }
}
