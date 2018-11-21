package info.ljungqvist.yaol


internal class FlatMappedObservable<T>(private val getter: () -> Observable<T>) : ObservableImpl<T>() {

    private val notifySuper: (T) -> Unit = { super.notifyChange() }

    private var ref: SettableReference<Observable<T>> = SettableReference.Unset
    private val delegate
        get() = ref.let({ it }, { getter() })
    private var subscription: Subscription? = null

    override val value: T
        get() = delegate.value

    override fun notifyChange() = synchronized(this) {
        val newDelegate = getter()

        if (ref.let({ it !== newDelegate }, { true })) {
            val update = ref.let({ it.value != newDelegate.value }, { true })
            subscription?.close()
            ref = SettableReference.Set(newDelegate)
            subscription = newDelegate.onChange(notifySuper)
            if (update) super.notifyChange()
        }
    }

    fun init() = synchronized(this) {
        ref.let({}, {
            val newDelegate = getter()
            ref = SettableReference.Set(newDelegate)
            subscription = newDelegate.onChange(notifySuper)
        })
    }

}
