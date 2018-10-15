package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

abstract class Observable<out T> {

    abstract val value: T

    private val subscriptions: AtomicReference<Set<Subscription<T>>> = AtomicReference(emptySet())
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
    }

    internal fun unsubscribe(subscription: Subscription<T>) {
        subscriptions.updateAndGet { a -> a - subscription }
    }

    fun onChange(body: (T) -> Unit): Subscription<*> =
            Subscription(this, body)
                    .also { subs -> subscriptions.updateAndGet { it + subs } }

    fun runAndOnChange(body: (T) -> Unit): Subscription<*> {
        body(value)
        return onChange(body)
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

    private fun addMappedObservables(observable: Observable<*>) {
        mappedObservables.updateAndGet { set ->
            set.asSequence().filter { it.get() != null }.toSet() + WeakReference(observable)
        }
    }

    fun <OUT> map(mapping: (T) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value) }
                    .also(::addMappedObservables)

    fun <A, OUT> join(other: Observable<A>, mapping: (T, A) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, other.value) }
                    .also(::addMappedObservables)

    fun <A, B, OUT> join(otherA: Observable<A>, otherB: Observable<B>, mapping: (T, A, B) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value) }
                    .also(::addMappedObservables)

    fun <A, B, C, OUT> join(otherA: Observable<A>, otherB: Observable<B>, otherC: Observable<C>, mapping: (T, A, B, C) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value) }
                    .also(::addMappedObservables)

    fun <A, B, C, D, OUT> join(otherA: Observable<A>, otherB: Observable<B>, otherC: Observable<C>, otherD: Observable<D>, mapping: (T, A, B, C, D) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value) }
                    .also(::addMappedObservables)

    fun <A, B, C, D, E, OUT> join(otherA: Observable<A>, otherB: Observable<B>, otherC: Observable<C>, otherD: Observable<D>, otherE: Observable<E>, mapping: (T, A, B, C, D, E) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value, otherE.value) }
                    .also(::addMappedObservables)

}

private class MappedObservable<OUT>(private val getter: () -> OUT) : Observable<OUT>() {

    override var value: OUT = getter()
        private set

    override fun notifyChange() {
        val newValue = getter()
        val update = value != newValue
        value = newValue
        if (update) super.notifyChange()
    }

}

open class MutableObservable<T>(initialValue: T) : Observable<T>() {

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

class Subscription<in T>(private val observable: Observable<T>, internal val onChange: (T) -> Unit) {

    fun unsubscribe() {
        observable.unsubscribe(this)
    }

}
