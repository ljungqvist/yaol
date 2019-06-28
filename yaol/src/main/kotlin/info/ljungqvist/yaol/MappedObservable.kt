package info.ljungqvist.yaol


internal open class MappedObservable<T>(private val getter: () -> T) : ObservableImpl<T>() {

    private var ref: SettableReference<T> = SettableReference.Unset

    override val value: T
        get() = ref.let({ it }) {
            synchronized(this) {
                getter().also { ref = SettableReference.Set(it) }
            }
        }

    override fun notifyChange() = synchronized(this) {
        val newValue = getter()
        val update = ref.let({ it != newValue }, { true })
        ref = SettableReference.Set(newValue)
        if (update) super.notifyChange()
    }

}
