package info.ljungqvist.yaol

fun <T> mutableObservable(value: T): MutableObservable<T> = object : MutableObservableImpl<T>(value) {}

open class MutableObservableImpl<T> protected constructor(initialValue: T) : ObservableImpl<T>(), MutableObservable<T> {

    override var value: T = initialValue
        set(value) = synchronized(this) {
            val update = field != value
            field = value
            if (update) notifyChange()
        }

}
