package info.ljungqvist.yaol

interface Subscription {
    fun unsubscribe()
}

internal class SubscriptionImpl<in T>(private val observable: Observable<T>, internal val onChange: (T) -> Unit) : Subscription {

    override fun unsubscribe() {
        observable.unsubscribe(this)
    }

}