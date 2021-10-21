package ru.hse.hseditor.domain.app

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import ru.hse.hseditor.domain.app.lifetimes.Lifetime

// TODO @thisisvolatile Reactive threading?

class Event<T>(
    val id: String,
    private val myLifetime: Lifetime? = null
) {
    init { myLifetime?.alsoOnTerminate { myListeners.clear() } }

    private val myListeners = mutableListOf<(T) -> Unit>()

    fun fire(withValue: T) = myListeners.forEach { it(withValue) }

    fun advise(lifetime: Lifetime, block: (T) -> Unit) {
        myListeners.add(block)
        lifetime.alsoOnTerminate { myListeners.remove(block) }
    }
}

class Property<T>(
    val id: String,
    initialValue: T,
    private val myLifetime: Lifetime? = null
) {
    private var myBackingValue = initialValue

    var value: T
        get() {
            return myBackingValue
        }
        set(newValue) {
            myBackingValue = newValue
            updatedEvent.fire(newValue)
        }

    val updatedEvent = Event<T>(id, myLifetime)
}

class DebounceEvent(
    val id: String,
    private val myLifetime: Lifetime,
    private val myEventDelayMs: Long,
) {
    private val myEvent = Event<Unit>("$id::Backing Event", myLifetime)

    fun adviseOnFinish(lifetime: Lifetime, block: (Unit) -> Unit) = myEvent.advise(lifetime, block)

    fun bounce() {
        myThrottleJob?.cancel()
        myThrottleJob = myLifetime.scopedLaunch {
            delay(myEventDelayMs)
            myEvent.fire(Unit)
        } ?: throw IllegalStateException("Lifetime terminated")
    }

    private var myThrottleJob: Job? = null
}