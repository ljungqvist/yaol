package info.ljungqvist.yaol

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface MutableObservable<T> : Observable<T> {

    override var value: T

    fun <OUT> twoWayMap(mapping: (T) -> OUT, reverseMapping: (OUT) -> T): MutableObservable<OUT> =
        TwoWayMappedObservable({ mapping(value) }, { value = reverseMapping(it) })
            .also(::addMappedObservables)

}

open class MutableObservableImpl<T> protected constructor(initialValue: T) : ObservableImpl<T>(), MutableObservable<T> {

    override var value: T = initialValue
        set(value) = synchronized(this) {
            val update = field != value
            field = value
            if (update) notifyChange()
        }

}

private class TwoWayMappedObservable<T>(getter: () -> T, private val setter: (T) -> Unit) : MappedObservable<T>(getter),
    MutableObservable<T> {
    override var value: T
        get() = super.value
        set(value) {
            setter(value)
        }
}

fun <T> mutableObservable(value: T): MutableObservable<T> = object : MutableObservableImpl<T>(value) {}

fun <T> mutableObservableProperty(mutableObservable: () -> MutableObservable<T>): ReadWriteProperty<Any, T> =
    object : ReadWriteProperty<Any, T> {
        override operator fun getValue(thisRef: Any, property: KProperty<*>): T = mutableObservable().value
        override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            mutableObservable().value = value
        }
    }