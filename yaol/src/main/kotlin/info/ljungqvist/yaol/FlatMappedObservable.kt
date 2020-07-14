package info.ljungqvist.yaol

internal abstract class AbstractFlatMappedObservable<T, OT : Observable<T>>(private val getter: () -> OT) : ObservableImpl<T>() {

    private val notifySuper: (T) -> Unit = { super.notifyChange() }

    protected var delegate = getter()

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

internal class FlatMappedObservable<T>(getter: () -> Observable<T>) : AbstractFlatMappedObservable<T, Observable<T>>(getter)

internal class MutableFlatMappedObservable<T>(getter: () -> MutableObservable<T>) : AbstractFlatMappedObservable<T, MutableObservable<T>>(getter), MutableObservable<T> {
    override var value: T
        get() = super.value
        set(value) {
            delegate.value = value
        }
}
