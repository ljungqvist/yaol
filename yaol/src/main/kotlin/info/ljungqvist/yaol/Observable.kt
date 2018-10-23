package info.ljungqvist.yaol

import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Timer {

    private var sum: Long = 0
    private var start: Long? = null

    private fun now() = System.nanoTime()

    fun start() {
        if (start != null) throw RuntimeException("Already running")
        start = now()
    }

    fun stop() {
        start
            ?.let {
                sum += now() - it
                start = null
            }
            ?: throw RuntimeException("Not running")
    }

    fun <T> time(body: () -> T): T {
        start()
        val res = body()
        stop()
        return res
    }

    val millis: Long
        get() =
            if (null == start) sum / 1000000
            else throw RuntimeException("Running")
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
        removeIf { it.get() == null }
    }
}

abstract class Observable<out T> {

    val timeNotifyChange = Timer()
    val timeNotifyChangeSubscriptions = Timer()
    val timeNotifyChangeWeakSubscriptions = Timer()
    val timeNotifyChangeMappedObservables = Timer()
    val timeUnsubscribe = Timer()
    val timeAddMappedObservables = Timer()

    abstract val value: T

//    private val subscriptions: AtomicReference<Set<SubscriptionImpl<T>>> = AtomicReference(emptySet())
//    private val weakSubscriptions: AtomicReference<Set<WeakReference<SubscriptionImpl<T>>>> =
//        AtomicReference(emptySet())
//    private val mappedObservables: AtomicReference<Set<WeakReference<out Observable<*>>>> = AtomicReference(emptySet())

    private val subscriptions: MutableSet<SubscriptionImpl<T>> = Collections.synchronizedSet(HashSet())
    private val weakSubscriptions: MutableSet<WeakReference<SubscriptionImpl<T>>> =
        Collections.synchronizedSet(HashSet())
    private val mappedObservables: MutableSet<WeakReference<Observable<*>>> = Collections.synchronizedSet(HashSet())

//    protected open fun notifyChange() {
//        mappedObservables
//            .updateAndGet { set ->
//                set.asSequence().filter { it.get() != null }.toSet()
//            }
//            .mapNotNull { it.get() }
//            .forEach {
//                it.notifyChange()
//            }
//        subscriptions.get().forEach {
//            it.onChange(value)
//        }
//        weakSubscriptions
//            .updateAndGet { set ->
//                set.asSequence().filter { it.get() != null }.toSet()
//            }
//            .mapNotNull { it.get() }
//            .forEach {
//                it.onChange(value)
//            }
//    }

    protected open fun notifyChange(): Unit = timeNotifyChange.time {
        synchronized(mappedObservables) {
            if (mappedObservables.size > 1) println("mappedObservables.size = ${mappedObservables.size}")
            timeNotifyChangeMappedObservables.time {
                mappedObservables.forEachSet { notifyChange() }
            }
        }
        synchronized(subscriptions) {
            timeNotifyChangeSubscriptions.time {
                subscriptions.forEach {
                    it.onChange(value)
                }
            }
        }
        synchronized(weakSubscriptions) {
            timeNotifyChangeWeakSubscriptions.time {
                weakSubscriptions.forEachSet { onChange(value) }
            }
        }
    }

    internal open fun unsubscribe(subscription: SubscriptionImpl<T>): Unit = timeUnsubscribe.time {
        subscriptions.remove(subscription)
        weakSubscriptions.removeIf { it.get() == null && it.get() === subscription }
        mappedObservables.removeIf { it.get() == null }
    }

    fun onChange(body: (T) -> Unit): Subscription =
        SubscriptionImpl(this, body)
            .also { subs -> subscriptions.add(subs) }

    fun onChangeWeak(body: (T) -> Unit): Subscription =
        SubscriptionImpl(this, body)
            .also { subs -> weakSubscriptions.add(WeakReference(subs)) }

    fun runAndOnChange(body: (T) -> Unit): Subscription {
        body(value)
        return onChange(body)
    }

    fun runAndOnChangeWeak(body: (T) -> Unit): Subscription {
        body(value)
        return onChangeWeak(body)
    }

    fun runAndOnChangeUntilTrue(body: (T) -> Boolean) {
        if (!body(value)) {
            selfReference<Subscription> {
                onChange {
                    if (body(it)) {
                        self.unsubscribe()
                    }
                }
            }
        }
    }

    fun runAndOnChangeUntilTrueWeak(body: (T) -> Boolean) {
        if (!body(value)) {
            selfReference<Subscription> {
                onChangeWeak {
                    if (body(it)) {
                        self.unsubscribe()
                    }
                }
            }
        }
    }

    internal fun addMappedObservables(observable: Observable<*>) {
        mappedObservables.add(WeakReference(observable))
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

fun <T> Observable<Observable<T>>.flatten(): Observable<T> = flatMap { it }

fun <T, OUT> List<Observable<T>>.join(mapping: (List<T>) -> OUT): Observable<OUT> =
    MappedObservable { mapping(map { it.value }) }
        .also { mapped ->
            forEach { it.addMappedObservables(mapped) }
        }

private class MappedObservable<T>(private val getter: () -> T) : Observable<T>() {

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

private class FlatMappedObservable<T>(private val getter: () -> Observable<T>) : Observable<T>() {

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

    override fun unsubscribe(subscription: SubscriptionImpl<T>) {
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

open class MutableObservable<T> protected constructor(initialValue: T) : Observable<T>() {

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

fun <T> mutableObservable(value: T): MutableObservable<T> = object : MutableObservable<T>(value) {}


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
