package info.ljungqvist.yaol

import java.util.concurrent.CountDownLatch
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * The main observable interface.
 * @param T the type of the observable
 */
interface Observable<out T> : ReadOnlyProperty<Any, T> {

    /**
     * The value of the observable
     */
    val value: T

    /**
     * Getter for [[ReadOnlyProperty]]
     * @return the value
     */
    override operator fun getValue(thisRef: Any, property: KProperty<*>): T = value

    /**
     * Notify the observable that the value has changed. Should cause the listeners to be executed.
     * Do not use from outside the observable!
     */
    /* protected */ fun notifyChange()

    /**
     * Add an observable depending on this observable to be notified when the value changes
     * Do not use from outside the observable!
     * @param observable the observable to be added
     */
    /* protected */ fun addMappedObservables(observable: Observable<*>)

    /**
     * Remove a subscription.
     * Only to be used by [[Subscription]]s
     * @param subscription the subscription to be removed
     */
    /* internal */ fun unsubscribe(subscription: Subscription)

    /**
     * Register a function to be run when the observables value changes.
     * Hold on to the [[Subscription]] to avoid it being GCd.
     *
     * @param body the function to be run
     * @return a subscription for unsubscribing
     */
    fun onChange(body: (T) -> Unit): Subscription

    /**
     * Register a function to be run once synchronously with the current value and then any time the
     * observables value changes.
     * Hold on to the [[Subscription]] to avoid it being GCd.
     *
     * @param body the function to be run
     * @return a subscription for unsubscribing
     */
    fun runAndOnChange(body: (T) -> Unit): Subscription =
            runAndOnChange(body, body)

    /**
     * Sanme as [runAndOnChange] (body), but with different body for the first run
     * @param bodyFirstRun the function for the first execution
     * @param body the function for the rest of the executions
     * @return a subscription for unsubscribing
     */
    fun runAndOnChange(bodyFirstRun: (T) -> Unit, body: (T) -> Unit): Subscription {
        val latch = CountDownLatch(1)
        val subscription = onChange {
            latch.await()
            body(it)
        }
        bodyFirstRun(value)
        latch.countDown()
        return subscription
    }

    /**
     * Register a boolean function to be run when the observables value changes, until it returns
     * true.
     * Hold on to the [[Subscription]] to avoid it being GCd.
     * The returned subscription will be unsubscribed once the function has returned true.
     * @param body the function to be run
     * @return a subscription for unsubscribing
     */
    fun onChangeUntilTrue(body: (T) -> Boolean): Subscription =
            selfReference {
                onChange {
                    if (body(it)) {
                        self.close()
                    }
                }
            }

    /**
     * Register a boolean function to be run once synchronously and then when the observables value changes, until it
     * returns true.
     * Hold on to the [[Subscription]] to avoid it being GCd.
     * The returned subscription will be unsubscribed once the function has returned true.
     * @param body the function to be run
     * @return a subscription for unsubscribing
     */
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

    /**
     * Maps the observable to an [OUT] observable.
     *
     * @param mapping the mapping function from the value of this observable to the value of the new observable
     * @return the mapped observable
     */
    fun <OUT> map(mapping: (T) -> OUT): Observable<OUT> =
            MappedObservable { mapping(value) }
                    .also(::addMappedObservables)

    /**
     * Flat maps observable to an [OUT] observable by an [T] -> [Observable] mapping.
     *
     * @param mapping the mapping function from the value of this observable to a new observable
     * @return the mapped observable
     */
    fun <OUT> flatMap(mapping: (T) -> Observable<OUT>): Observable<OUT> =
            FlatMappedObservable { mapping(value) }
                    .also(::addMappedObservables)
                    .also { it.init() }

    /**
     * Same as [flatMap], bbut the mapping function may return null, resulting in an immutable observable with the
     * value null
     *
     * @param mapping the mapping function
     * @return the mapped observable
     */
    fun <OUT> flatMapNullable(mapping: (T) -> Observable<OUT>?): Observable<OUT?> =
            FlatMappedObservable { mapping(value) ?: immutableObservable(null) }
                    .also(::addMappedObservables)
                    .also { it.init() }

    /**
     * Joins this and another observable into new observable.
     * @param other the other observable
     * @param mapping the function to join the observables
     * @return the joined observable
     */
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
