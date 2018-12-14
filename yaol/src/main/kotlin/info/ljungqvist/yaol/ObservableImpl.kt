package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.*

abstract class ObservableImpl<out T> : Observable<T> {

    private val subscriptions: MutableSet<SubscriptionImpl<T>> = Collections.synchronizedSet(HashSet())
    private val mappedObservables: MutableSet<WeakReference<Observable<*>>> = HashSet()

    private var inNotifyChange = false

    override fun notifyChange() {
        synchronized(mappedObservables) {
            if (inNotifyChange) throw IllegalStateException("notifyChange called from inside notifyChange")
            inNotifyChange = true
            callMappedOnChange()
            inNotifyChange = false
        }
        synchronized(subscriptions) {
            subscriptions.toList()
        }.forEach {
            it.onChange(value)
        }
    }

    override fun unsubscribe(subscription: Subscription) {
        subscriptions.remove(subscription)
    }

    override fun onChange(body: (T) -> Unit): Subscription =
        SubscriptionImpl(this, body)
            .also { subs -> subscriptions.add(subs) }

    override fun addMappedObservables(observable: Observable<*>): Unit = synchronized(mappedObservables) {
        if (inNotifyChange) throw IllegalStateException("addMappedObservables called from inside notifyChange")

        if (mappedObservables.none { it.get() == observable }) {
            mappedObservables.add(WeakReference(observable))
        }
    }

    private fun callMappedOnChange() {
        var setSize = 0
        mappedObservables
            .asSequence()
            .mapNotNull { it.get() }
            .forEach {
                setSize++
                it.notifyChange()
            }
        val size = mappedObservables.size
        if (
            when {
                setSize < 10 -> size > 20
                else -> size / setSize > 2
            }
        ) {
            mappedObservables.remove { it.get() == null }
        }
    }

}

/**
 * Same as [java.util.Collection.removeIf] in Java 1.8
 */
private fun <T> MutableCollection<T>.remove(filter: (T) -> Boolean): Int {

    var removed = 0
    val it = iterator()
    while (it.hasNext()) {
        if (filter(it.next())) {
            it.remove()
            removed++
        }
    }
    return removed

}
