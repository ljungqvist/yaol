package info.ljungqvist.yaol


class Subscription<in T>(private val observable: Observable<T>, internal val onChange: (T) -> Unit) {

    fun unsubscribe() {
        observable.unsubscribe(this)
    }

}