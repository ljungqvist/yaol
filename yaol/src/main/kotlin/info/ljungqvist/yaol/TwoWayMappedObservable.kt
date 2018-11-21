package info.ljungqvist.yaol


internal class TwoWayMappedObservable<T>(getter: () -> T, private val setter: (T) -> Unit) : MappedObservable<T>(getter),
        MutableObservable<T> {
    override var value: T
        get() = super.value
        set(value) {
            setter(value)
        }
}
