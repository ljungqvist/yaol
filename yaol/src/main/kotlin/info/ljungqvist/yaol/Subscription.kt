package info.ljungqvist.yaol

import java.io.Closeable

interface Subscription : Closeable {
    fun isClosed(): Boolean
}

internal class SubscriptionImpl<in T>(observable: Observable<T>, onChange: (T) -> Unit) :
    Subscription {

    private var data: Data<T>? = Data(observable, onChange)

    override fun close() {
        data?.let { (observable, _) ->
            observable.unsubscribe(this)
        }
        data = null
    }

    override fun isClosed(): Boolean = data == null

    internal fun onChange(value: T) = data
        ?.let { (_, onChange) -> onChange(value) }
        ?: throw IllegalStateException("Subscription has already been closed")

    private data class Data<T>(val observable: Observable<T>, val onChange: (T) -> Unit)

}