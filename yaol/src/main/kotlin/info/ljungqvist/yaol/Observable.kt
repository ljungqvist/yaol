package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.properties.ReadOnlyProperty
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

interface Observable<out T> {

    val value: T

    fun notifyChange()

    fun onChange(body: (T) -> Unit): Subscription

    fun addMappedObservables(observable: Observable<*>)

    fun unsubscribe(subscription: Subscription)

    fun runAndOnChange(body: (T) -> Unit): Subscription {
        val latch = CountDownLatch(1)
        val subscription = onChange {
            latch.await()
            body(it)
        }
        body(value)
        latch.countDown()
        return subscription
    }

    fun onChangeUntilTrue(body: (T) -> Boolean): Subscription =
        selfReference {
            onChange {
                if (body(it)) {
                    self.close()
                }
            }
        }

    fun runAndOnChangeUntilTrue(body: (T) -> Boolean): Subscription {
        val latch = CountDownLatch(1)
        var ready = false
        val subscription = onChangeUntilTrue {
            latch.await()
            ready || body(it)
        }
        ready = body(value)
        latch.countDown()
        return subscription
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

internal open class MappedObservable<T>(private val getter: () -> T) : ObservableImpl<T>() {

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
            subscription?.close()
            ref = SettableReference.Set(newDelegate)
            subscription = newDelegate.onChange(notifySuper)
            if (update) super.notifyChange()
        }
    }

    override fun unsubscribe(subscription: Subscription) {
        this.subscription?.close()
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


fun <T> observable(value: T): Observable<T> = object : ObservableImpl<T>() {
    override val value: T = value
}


fun <T> observableProperty(observable: () -> Observable<T>): ReadOnlyProperty<Any, T> =
    object : ReadOnlyProperty<Any, T> {
        override operator fun getValue(thisRef: Any, property: KProperty<*>): T = observable().value
    }
