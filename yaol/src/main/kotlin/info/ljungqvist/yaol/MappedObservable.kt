package info.ljungqvist.yaol


internal open class MappedObservable<T>(private val getter: () -> T) : ObservableImpl<T>() {

    private var ref: SettableReference<T> = SettableReference.Unset

    override val value: T
        get() = synchronized(this) {
            ref.let(
                { it },
                { getter().also { ref = SettableReference.Set(it) } }
            )
        }

    override fun notifyChange() {
        val update: Boolean
        synchronized(this) {
            val newValue = getter()
            update = ref.let({ it != newValue }, { true })
            ref = SettableReference.Set(newValue)
        }
        if (update) super.notifyChange()
    }

}
