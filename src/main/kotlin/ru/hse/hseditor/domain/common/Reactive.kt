package ru.hse.hseditor.domain.common

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import ru.hse.hseditor.domain.common.lifetimes.Lifetime

// TODO @thisisvolatile Reactive threading?

fun tickerFlow(period: Long, initDelay: Long = 0) = flow {
    delay(initDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

fun <T> MutableList<T>.addLifetimed(lifetime: Lifetime, it: T) = lifetime.alsoBracket(
    { add(it) },
    { remove(it) }
)

class Event<T>(
    val id: String,
    private val myLifetime: Lifetime? = null
) {
    init {
        myLifetime?.alsoOnTerminate { myListeners.clear() }
    }

    private val myListeners = mutableListOf<(T) -> Unit>()

    fun fire(withValue: T) = myListeners.forEach { it(withValue) }

    fun advise(lifetime: Lifetime, block: (T) -> Unit) {
        myListeners.add(block)
        lifetime.alsoOnTerminate { myListeners.remove(block) }
    }
}

enum class ChangeKind { ADD, REMOVE }

data class ObservableChange<T>(val kind: ChangeKind, val it: T)

class ObservableSet<T>(private val mySet: MutableSet<T> = mutableSetOf()) : MutableSet<T> by mySet {
    val addRemove = Event<ObservableChange<T>>("ObservableSet::addRemove")

    fun addFiring(item: T) {
        addRemove.fire(ObservableChange(ChangeKind.ADD, item))
        mySet.add(item)
    }

    fun removeFiring(item: T) {
        addRemove.fire(ObservableChange(ChangeKind.REMOVE, item))
        mySet.remove(item)
    }
}

class ObservableList<T>(private val myList: MutableList<T> = mutableListOf()) : MutableList<T> by myList {
    val addRemove = Event<ObservableChange<T>>("ObservableList::addRemove")
    val onNewState = Event<List<T>>("ObservableList::onNewState")

    fun addFiring(item: T) {
        addRemove.fire(ObservableChange(ChangeKind.ADD, item))
        myList.add(item)
        onNewState.fire(myList)
    }

    fun removeFiring(item: T) {
        addRemove.fire(ObservableChange(ChangeKind.REMOVE, item))
        myList.remove(item)
        onNewState.fire(myList)
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