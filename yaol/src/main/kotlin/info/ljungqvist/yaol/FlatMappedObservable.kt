package info.ljungqvist.yaol


internal class FlatMappedObservable<T>(private val getter: () -> Observable<T>) : ObservableImpl<T>() {

    private val notifySuper: (T) -> Unit = { super.notifyChange() }

    private var delegate = getter()

    private var subscription: Subscription = delegate.onChange(notifySuper)

    override val value: T
        get() = delegate.value

    override fun notifyChange() = synchronized(this) {
        val newDelegate = getter()

        if (delegate !== newDelegate) {
            val update = delegate.value != newDelegate.value
            subscription.close()
            delegate = newDelegate
            subscription = newDelegate.onChange(notifySuper)
            if (update) super.notifyChange()
        }
    }

}
