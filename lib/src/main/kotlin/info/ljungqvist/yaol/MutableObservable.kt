package info.ljungqvist.yaol

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

typealias ReverseMapping<FROM, TO> = (currentValue: FROM, mappedValue: TO) -> FROM

@Suppress("DestructuringWrongName")
interface MutableObservable<T> : Observable<T>, ReadWriteProperty<Any?, T> {

    override var value: T

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun <OUT> twoWayMap(mapping: (T) -> OUT, reverseMapping: ReverseMapping<T, OUT>): MutableObservable<OUT> =
        TwoWayMappedObservable({ mapping(value) }, { value = reverseMapping(value, it) })
            .also(::addMappedObservables)

    fun <A, OUT> twoWayJoin(
        other: MutableObservable<A>,
        mapping: (T, A) -> OUT,
        reverseMapping: ReverseMapping<Data2<T, A>, OUT>
    ): MutableObservable<OUT> =
        TwoWayMappedObservable(
            { mapping(value, other.value) },
            {
                reverseMapping(data(value, other.value), it).let { (t, a) ->
                    value = t
                    other.value = a
                }
            }
        ).also { mapped ->
            listOf(this, other).forEach { it.addMappedObservables(mapped) }
        }

    fun <A, B, OUT> twoWayJoin(
        otherA: MutableObservable<A>,
        otherB: MutableObservable<B>,
        mapping: (T, A, B) -> OUT,
        reverseMapping: ReverseMapping<Data3<T, A, B>, OUT>
    ): MutableObservable<OUT> =
        TwoWayMappedObservable(
            { mapping(value, otherA.value, otherB.value) },
            {
                reverseMapping(data(value, otherA.value, otherB.value), it).let { (t, a, b) ->
                    value = t
                    otherA.value = a
                    otherB.value = b
                }
            }
        ).also { mapped ->
            listOf(this, otherA, otherB).forEach { it.addMappedObservables(mapped) }
        }

    fun <A, B, C, OUT> twoWayJoin(
        otherA: MutableObservable<A>,
        otherB: MutableObservable<B>,
        otherC: MutableObservable<C>,
        mapping: (T, A, B, C) -> OUT,
        reverseMapping: ReverseMapping<Data4<T, A, B, C>, OUT>
    ): MutableObservable<OUT> =
        TwoWayMappedObservable(
            { mapping(value, otherA.value, otherB.value, otherC.value) },
            {
                reverseMapping(data(value, otherA.value, otherB.value, otherC.value), it).let { (t, a, b, c) ->
                    value = t
                    otherA.value = a
                    otherB.value = b
                    otherC.value = c
                }
            }
        ).also { mapped ->
            listOf(this, otherA, otherB, otherC).forEach { it.addMappedObservables(mapped) }
        }

    fun <A, B, C, D, OUT> twoWayJoin(
        otherA: MutableObservable<A>,
        otherB: MutableObservable<B>,
        otherC: MutableObservable<C>,
        otherD: MutableObservable<D>,
        mapping: (T, A, B, C, D) -> OUT,
        reverseMapping: ReverseMapping<Data5<T, A, B, C, D>, OUT>
    ): MutableObservable<OUT> =
        TwoWayMappedObservable(
            { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value) },
            {
                reverseMapping(
                    data(value, otherA.value, otherB.value, otherC.value, otherD.value),
                    it
                ).let { (t, a, b, c, d) ->
                    value = t
                    otherA.value = a
                    otherB.value = b
                    otherC.value = c
                    otherD.value = d
                }
            }
        ).also { mapped ->
            listOf(this, otherA, otherB, otherC, otherD).forEach { it.addMappedObservables(mapped) }
        }

    fun <A, B, C, D, E, OUT> twoWayJoin(
        otherA: MutableObservable<A>,
        otherB: MutableObservable<B>,
        otherC: MutableObservable<C>,
        otherD: MutableObservable<D>,
        otherE: MutableObservable<E>,
        mapping: (T, A, B, C, D, E) -> OUT,
        reverseMapping: ReverseMapping<Data6<T, A, B, C, D, E>, OUT>
    ): MutableObservable<OUT> =
        TwoWayMappedObservable(
            { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value, otherE.value) },
            {
                reverseMapping(
                    data(value, otherA.value, otherB.value, otherC.value, otherD.value, otherE.value),
                    it
                ).let { (t, a, b, c, d, e) ->
                    value = t
                    otherA.value = a
                    otherB.value = b
                    otherC.value = c
                    otherD.value = d
                    otherE.value = e
                }
            }
        ).also { mapped ->
            listOf(this, otherA, otherB, otherC, otherD, otherE).forEach { it.addMappedObservables(mapped) }
        }

}




