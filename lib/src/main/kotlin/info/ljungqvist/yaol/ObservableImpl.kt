package info.ljungqvist.yaol

import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.ref.WeakReference
import java.util.*

abstract class ObservableImpl<out T> : Observable<T> {

    private val logger = KotlinLogging.logger { }

    private val subscriptions: MutableSet<SubscriptionImpl<T>> = Collections.synchronizedSet(HashSet())
    private val mappedObservables: MutableSet<WeakReference<Observable<*>>> = HashSet()

    private var inNotifyChange = false

    override fun notifyChange() {
        synchronized(mappedObservables) {
            if (inNotifyChange) throw IllegalStateException("notifyChange called from inside notifyChange")
            inNotifyChange = true
            try {
                callMappedOnChange()
            } catch (e: Exception) {
                logger.error(e) { "callMappedOnChange failed" }
                throw e
            } finally {
                inNotifyChange = false
            }
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
        val setSize =
            mappedObservables
                .asSequence()
                .mapNotNull { it.get() }
                .fold(0) { size, observable ->
                    observable.notifyChange()
                    size + 1
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
