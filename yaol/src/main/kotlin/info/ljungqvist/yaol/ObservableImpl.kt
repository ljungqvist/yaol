package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.*

abstract class ObservableImpl<out T> : Observable<T> {

    private val subscriptions: MutableSet<SubscriptionImpl<T>> = Collections.synchronizedSet(HashSet())
    private val mappedObservables: MutableSet<WeakReference<Observable<*>>> = Collections.synchronizedSet(HashSet())

    override fun notifyChange() {
        mappedObservables.forEachSet { notifyChange() }
        synchronized(subscriptions) {
            subscriptions.toList()
        }.forEach {
            it.onChange(value)
        }
    }

    override fun unsubscribe(subscription: Subscription) {
        subscriptions.remove(subscription)
        mappedObservables.removeIfSynchronized { it.get() == null }
    }

    override fun onChange(body: (T) -> Unit): Subscription =
            SubscriptionImpl(this, body)
                    .also { subs -> subscriptions.add(subs) }

    override fun addMappedObservables(observable: Observable<*>) {
        mappedObservables.add(WeakReference(observable))
    }

}


private inline fun <T> MutableSet<T>.removeIfSynchronized(predicate: (T) -> Boolean): Unit = synchronized(this) {
    iterator().let {
        while (it.hasNext())
            if (predicate(it.next()))
                it.remove()
    }
}

private inline fun <T : Any> MutableSet<WeakReference<T>>.forEachSet(f: T.() -> Unit): Unit = synchronized(this) {
    var setSize = 0
    asSequence()
            .mapNotNull { it.get() }
            .forEach {
                setSize++
                it.f()
            }
    if (
            when {
                setSize < 10 -> size > 20
                else -> size / setSize > 2
            }
    ) {
        removeIfSynchronized { it.get() == null }
    }
}