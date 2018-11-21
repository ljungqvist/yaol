package info.ljungqvist.yaol


fun <T> immutableObservable(value: T): Observable<T> = ImmutableObservable(value)

private class ImmutableObservable<T>(override val value: T) : Observable<T> {

    override fun notifyChange() = Unit

    override fun onChange(body: (T) -> Unit): Subscription =
            SubscriptionImpl(this, body)

    override fun addMappedObservables(observable: Observable<*>) = Unit

    override fun unsubscribe(subscription: Subscription) = Unit

    override fun <OUT> map(mapping: (T) -> OUT): Observable<OUT> =
            immutableObservable(mapping(value))

    override fun <OUT> flatMap(mapping: (T) -> Observable<OUT>): Observable<OUT> =
            mapping(value)

    override fun <OUT> flatMapNullable(mapping: (T) -> Observable<OUT>?): Observable<OUT?> =
            mapping(value) ?: immutableObservable(null)

    override fun <A, OUT> join(other: Observable<A>, mapping: (T, A) -> OUT): Observable<OUT> =
            other.map { mapping(value, it) }

    override fun <A, B, OUT> join(
            otherA: Observable<A>,
            otherB: Observable<B>,
            mapping: (T, A, B) -> OUT
    ): Observable<OUT> =
            otherA.join(otherB) { a, b ->
                mapping(value, a, b)
            }

    override fun <A, B, C, OUT> join(
            otherA: Observable<A>,
            otherB: Observable<B>,
            otherC: Observable<C>,
            mapping: (T, A, B, C) -> OUT
    ): Observable<OUT> =
            otherA.join(otherB, otherC) { a, b, c ->
                mapping(value, a, b, c)
            }

    override fun <A, B, C, D, OUT> join(
            otherA: Observable<A>,
            otherB: Observable<B>,
            otherC: Observable<C>,
            otherD: Observable<D>,
            mapping: (T, A, B, C, D) -> OUT
    ): Observable<OUT> =
            otherA.join(otherB, otherC, otherD) { a, b, c, d ->
                mapping(value, a, b, c, d)
            }

    override fun <A, B, C, D, E, OUT> join(
            otherA: Observable<A>,
            otherB: Observable<B>,
            otherC: Observable<C>,
            otherD: Observable<D>,
            otherE: Observable<E>,
            mapping: (T, A, B, C, D, E) -> OUT
    ): Observable<OUT> =
            otherA.join(otherB, otherC, otherD, otherE) { a, b, c, d, e ->
                mapping(value, a, b, c, d, e)
            }

}
