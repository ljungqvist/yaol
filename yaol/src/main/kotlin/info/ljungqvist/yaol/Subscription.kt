package info.ljungqvist.yaol

import java.io.Closeable

interface Subscription : Closeable {
    @Deprecated("use #close() instead", replaceWith = ReplaceWith("close()"))
    fun unsubscribe()
}

internal class SubscriptionImpl<in T>(private val observable: Observable<T>, internal val onChange: (T) -> Unit) :
    Subscription {

    override fun close() {
        observable.unsubscribe(this)
    }

    override fun unsubscribe() {
        close()
    }

}