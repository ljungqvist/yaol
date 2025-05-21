package info.ljungqvist.yaol


internal open class MappedObservable<T>(private val getter: () -> T) : ObservableImpl<T>() {

    private var internalValue = getter()

    override val value: T
        get() = internalValue

    override fun notifyChange() {
        val newValue = getter()
        val update = internalValue != newValue
        internalValue = newValue
        if (update) super.notifyChange()
    }

}
