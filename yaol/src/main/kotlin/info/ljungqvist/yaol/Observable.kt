package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Observable<out T> {

    abstract val value: T

    private val subscriptions: AtomicReference<Set<Subscription<T>>> = AtomicReference(emptySet())
    private val weakSubscriptions: AtomicReference<Set<WeakReference<Subscription<T>>>> = AtomicReference(emptySet())
    private val mappedObservables: AtomicReference<Set<WeakReference<out Observable<*>>>> = AtomicReference(emptySet())

    protected open fun notifyChange() {
        mappedObservables
                .updateAndGet { set ->
                    set.asSequence().filter { it.get() != null }.toSet()
                }
                .mapNotNull { it.get() }
                .forEach {
                    it.notifyChange()
                }
        subscriptions.get().forEach {
            it.onChange(value)
        }
        weakSubscriptions
                .updateAndGet { set ->
                    set.asSequence().filter { it.get() != null }.toSet()
                }
                .mapNotNull { it.get() }
                .forEach {
                    it.onChange(value)
                }
    }

    internal open fun unsubscribe(subscription: Subscription<T>) {
        subscriptions.updateAndGet { a -> a - subscription }
        weakSubscriptions
                .updateAndGet { set ->
                    set.asSequence()
                            .filter { it.get() != null && it.get() !== subscription }
                            .toSet()
                }
    }

    fun onChange(body: (T) -> Unit): Subscription<*> =
            Subscription(this, body)
                    .also { subs -> subscriptions.updateAndGet { it + subs } }

    fun onChangeWeak(body: (T) -> Unit): Subscription<*> =
            Subscription(this, body)
                    .also { subs -> weakSubscriptions.updateAndGet { it + WeakReference(subs) } }

    fun runAndOnChange(body: (T) -> Unit): Subscription<*> {
        body(value)
        return onChange(body)
    }

    fun runAndOnChangeWeak(body: (T) -> Unit): Subscription<*> {
        body(value)
        return onChangeWeak(body)
    }

    fun runAndOnChangeUnitTrue(body: (T) -> Boolean) {
        if (!body(value)) {
            selfReference<Subscription<*>> {
                onChange {
                    if (body(it)) {
                        self.unsubscribe()
                    }
                }
            }
        }
    }

    fun runAndOnChangeUnitTrueWeak(body: (T) -> Boolean) {
        if (!body(value)) {
            selfReference<Subscription<*>> {
                onChangeWeak {
                    if (body(it)) {
                        self.unsubscribe()
                    }
                }
            }
        }
    }

    private fun addMappedObservables(observable: Observable<*>) {
        mappedObservables.updateAndGet { set ->
            set.asSequence().filter { it.get() != null }.toSet() + WeakReference(observable)
        }
    }

    fun <OUT> map(mapping: (T) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value) }
                    .also(::addMappedObservables)

    fun <OUT> flatMap(mapping: (T) -> Observable<OUT>): Observable<OUT> =
            FlatMappedObservable { mapping(value) }
                    .also(::addMappedObservables)

    fun <OUT> flatMapNullable(mapping: (T) -> Observable<OUT>?): Observable<OUT?> =
            FlatMappedObservable { mapping(value) ?: observable(null) }
                    .also(::addMappedObservables)

    fun <A, OUT> join(other: Observable<A>, mapping: (T, A) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, other.value) }
                    .also { mapped ->
                        listOf(this, other).forEach { it.addMappedObservables(mapped) }
                    }

    fun <A, B, OUT> join(otherA: Observable<A>, otherB: Observable<B>, mapping: (T, A, B) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value) }
                    .also { mapped ->
                        listOf(this, otherA, otherB).forEach { it.addMappedObservables(mapped) }
                    }

    fun <A, B, C, OUT> join(otherA: Observable<A>, otherB: Observable<B>, otherC: Observable<C>, mapping: (T, A, B, C) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value) }
                    .also { mapped ->
                        listOf(this, otherA, otherB, otherC).forEach { it.addMappedObservables(mapped) }
                    }

    fun <A, B, C, D, OUT> join(otherA: Observable<A>, otherB: Observable<B>, otherC: Observable<C>, otherD: Observable<D>, mapping: (T, A, B, C, D) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value) }
                    .also { mapped ->
                        listOf(this, otherA, otherB, otherC, otherD).forEach { it.addMappedObservables(mapped) }
                    }

    fun <A, B, C, D, E, OUT> join(otherA: Observable<A>, otherB: Observable<B>, otherC: Observable<C>, otherD: Observable<D>, otherE: Observable<E>, mapping: (T, A, B, C, D, E) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value, otherE.value) }
                    .also { mapped ->
                        listOf(this, otherA, otherB, otherC, otherD, otherE).forEach { it.addMappedObservables(mapped) }
                    }

}

fun <T> Observable<Observable<T>>.flatten(): Observable<T> = flatMap { it }

private class MappedObservable<T>(private val getter: () -> T) : Observable<T>() {

    override var value: T = getter()
        private set

    override fun notifyChange() {
        val newValue = getter()
        val update = value != newValue
        value = newValue
        if (update) super.notifyChange()
    }

}

private class FlatMappedObservable<T>(private val getter: () -> Observable<T>) : Observable<T>() {

    private var delegate = getter()
    private var subscription = delegate.onChange { super.notifyChange() }

    override val value: T
        get() = delegate.value

    override fun notifyChange() {
        val newDelegate = getter()
        if (delegate !== newDelegate) {
            val newValue = newDelegate.value
            val update = value != newValue
            subscription.unsubscribe()
            delegate = newDelegate
            subscription = delegate.onChange { super.notifyChange() }
            if (update) super.notifyChange()
        }
    }

    override fun unsubscribe(subscription: Subscription<T>) {
        this.subscription.unsubscribe()
        super.unsubscribe(subscription)
    }

}

open class MutableObservable<T> internal constructor(initialValue: T) : Observable<T>() {

    override var value: T = initialValue
        set(value) {
            val update = field != value
            field = value
            if (update) notifyChange()
        }

}

fun <T> observable(value: T): Observable<T> = object : Observable<T>() {
    override val value: T = value
}

fun <T> mutableObservable(value: T): MutableObservable<T> = MutableObservable(value)


fun <T> observableProperty(observable: () -> Observable<T>): ReadOnlyProperty<Any, T> = object : ReadOnlyProperty<Any, T> {
    override operator fun getValue(thisRef: Any, property: KProperty<*>): T = observable().value
}

fun <T> mutableObservableProperty(mutableObservable: () -> MutableObservable<T>): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {
    override operator fun getValue(thisRef: Any, property: KProperty<*>): T = mutableObservable().value
    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        mutableObservable().value = value
    }
}
