package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private inline fun <T> MutableSet<T>.removeIfSynchronized(predicate: (T) -> Boolean): Unit = synchronized(this) {
    iterator().let {
        while (it.hasNext())
            if (predicate(it.next()))
                it.remove()
    }
}

private inline fun <T : Any> MutableSet<WeakReference<T>>.forEachSet(f: T.() -> Unit): Unit = synchronized(this) {
    var setSize = 0
    asSequence()
        .mapNotNull { it.get() }
        .forEach {
            setSize++
            it.f()
        }
    if (
        when {
            setSize < 10 -> size > 20
            else -> size / setSize > 2
        }
    ) {
        removeIfSynchronized { it.get() == null }
    }
}

private val onChangeUntilTrueReferenceHolder: MutableSet<Subscription> = Collections.synchronizedSet(HashSet())

interface Observable<out T> {

    val value: T

    fun notifyChange()

    fun onChange(body: (T) -> Unit): Subscription

    fun addMappedObservables(observable: Observable<*>)

    fun unsubscribe(subscription: Subscription)

    fun runAndOnChange(body: (T) -> Unit): Subscription {
        body(value)
        return onChange(body)
    }

    fun onChangeUntilTrue(body: (T) -> Boolean) {
        onChangeUntilTrueReferenceHolder +=
                selfReference<Subscription> {
                    onChange {
                        if (body(it)) {
                            self.unsubscribe()
                            onChangeUntilTrueReferenceHolder -= self
                        }
                    }
                }
    }

    fun runAndOnChangeUntilTrue(body: (T) -> Boolean) {
        if (!body(value)) {
            onChangeUntilTrue(body)
        }
    }

    fun <OUT> map(mapping: (T) -> OUT): Observable<OUT> =
        MappedObservable { mapping(value) }
            .also(::addMappedObservables)

    fun <OUT> flatMap(mapping: (T) -> Observable<OUT>): Observable<OUT> =
        FlatMappedObservable { mapping(value) }
            .also(::addMappedObservables)
            .also { it.init() }

    fun <OUT> flatMapNullable(mapping: (T) -> Observable<OUT>?): Observable<OUT?> =
        FlatMappedObservable { mapping(value) ?: observable(null) }
            .also(::addMappedObservables)
            .also { it.init() }

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

    fun <A, B, C, OUT> join(
        otherA: Observable<A>,
        otherB: Observable<B>,
        otherC: Observable<C>,
        mapping: (T, A, B, C) -> OUT
    ): Observable<OUT> =
        MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value) }
            .also { mapped ->
                listOf(this, otherA, otherB, otherC).forEach { it.addMappedObservables(mapped) }
            }

    fun <A, B, C, D, OUT> join(
        otherA: Observable<A>,
        otherB: Observable<B>,
        otherC: Observable<C>,
        otherD: Observable<D>,
        mapping: (T, A, B, C, D) -> OUT
    ): Observable<OUT> =
        MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value) }
            .also { mapped ->
                listOf(this, otherA, otherB, otherC, otherD).forEach { it.addMappedObservables(mapped) }
            }

    fun <A, B, C, D, E, OUT> join(
        otherA: Observable<A>,
        otherB: Observable<B>,
        otherC: Observable<C>,
        otherD: Observable<D>,
        otherE: Observable<E>,
        mapping: (T, A, B, C, D, E) -> OUT
    ): Observable<OUT> =
        MappedObservable { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value, otherE.value) }
            .also { mapped ->
                listOf(this, otherA, otherB, otherC, otherD, otherE).forEach { it.addMappedObservables(mapped) }
            }

}

abstract class ObservableImpl<out T> : Observable<T> {

    private val subscriptions: MutableSet<SubscriptionImpl<T>> = Collections.synchronizedSet(HashSet())
    private val mappedObservables: MutableSet<WeakReference<Observable<*>>> = Collections.synchronizedSet(HashSet())

    override fun notifyChange() {
        mappedObservables.forEachSet { notifyChange() }
        synchronized(subscriptions) {
            subscriptions.toList()
        }.forEach {
            it.onChange(value)
        }
    }

    override fun unsubscribe(subscription: Subscription) {
        subscriptions.remove(subscription)
        mappedObservables.removeIfSynchronized { it.get() == null }
    }

    override fun onChange(body: (T) -> Unit): Subscription =
        SubscriptionImpl(this, body)
            .also { subs -> subscriptions.add(subs) }

    override fun addMappedObservables(observable: Observable<*>) {
        mappedObservables.add(WeakReference(observable))
    }

}

fun <T> Observable<Observable<T>>.flatten(): Observable<T> = flatMap { it }

fun <T, OUT> List<Observable<T>>.join(mapping: (List<T>) -> OUT): Observable<OUT> =
    MappedObservable { mapping(map { it.value }) }
        .also { mapped ->
            forEach { it.addMappedObservables(mapped) }
        }

private open class MappedObservable<T>(private val getter: () -> T) : ObservableImpl<T>() {

    private var ref: SettableReference<T> = SettableReference.Unset

    override val value: T
        get() = ref.let({ it }, { getter() })

    override fun notifyChange() = synchronized(this) {
        val newValue = getter()
        val update = ref.let({ it != newValue }, { true })
        ref = SettableReference.Set(newValue)
        if (update) super.notifyChange()
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

private class FlatMappedObservable<T>(private val getter: () -> Observable<T>) : ObservableImpl<T>() {

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
            subscription?.unsubscribe()
            ref = SettableReference.Set(newDelegate)
            subscription = newDelegate.onChange(notifySuper)
            if (update) super.notifyChange()
        }
    }

    override fun unsubscribe(subscription: Subscription) {
        this.subscription?.unsubscribe()
        super.unsubscribe(subscription)
    }

    fun init() = synchronized(this) {
        ref.let({}, {
            val newDelegate = getter()
            ref = SettableReference.Set(newDelegate)
            subscription = newDelegate.onChange(notifySuper)
        })
    }

}

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

fun <T> observable(value: T): Observable<T> = object : ObservableImpl<T>() {
    override val value: T = value
}

fun <T> mutableObservable(value: T): MutableObservable<T> = object : MutableObservableImpl<T>(value) {}


fun <T> observableProperty(observable: () -> Observable<T>): ReadOnlyProperty<Any, T> =
    object : ReadOnlyProperty<Any, T> {
        override operator fun getValue(thisRef: Any, property: KProperty<*>): T = observable().value
    }

fun <T> mutableObservableProperty(mutableObservable: () -> MutableObservable<T>): ReadWriteProperty<Any, T> =
    object : ReadWriteProperty<Any, T> {
        override operator fun getValue(thisRef: Any, property: KProperty<*>): T = mutableObservable().value
        override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            mutableObservable().value = value
        }
    }
