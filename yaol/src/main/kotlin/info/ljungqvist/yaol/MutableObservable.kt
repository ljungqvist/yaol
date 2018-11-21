package info.ljungqvist.yaol

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface MutableObservable<T> : Observable<T>, ReadWriteProperty<Any, T> {

    override var value: T

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T = value

    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun <OUT> twoWayMap(mapping: (T) -> OUT, reverseMapping: (OUT) -> T): MutableObservable<OUT> =
            TwoWayMappedObservable({ mapping(value) }, { value = reverseMapping(it) })
                    .also(::addMappedObservables)

    fun <A, OUT> twoWayJoin(
            other: MutableObservable<A>,
            mapping: (T, A) -> OUT,
            reverseMapping: (OUT) -> Data2<T, A>
    ): MutableObservable<OUT> =
            TwoWayMappedObservable(
                    { mapping(value, other.value) },
                    {
                        reverseMapping(it).let { (t, a) ->
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
            reverseMapping: (OUT) -> Data3<T, A, B>
    ): MutableObservable<OUT> =
            TwoWayMappedObservable(
                    { mapping(value, otherA.value, otherB.value) },
                    {
                        reverseMapping(it).let { (t, a, b) ->
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
            reverseMapping: (OUT) -> Data4<T, A, B, C>
    ): MutableObservable<OUT> =
            TwoWayMappedObservable(
                    { mapping(value, otherA.value, otherB.value, otherC.value) },
                    {
                        reverseMapping(it).let { (t, a, b, c) ->
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
            reverseMapping: (OUT) -> Data5<T, A, B, C, D>
    ): MutableObservable<OUT> =
            TwoWayMappedObservable(
                    { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value) },
                    {
                        reverseMapping(it).let { (t, a, b, c, d) ->
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
            reverseMapping: (OUT) -> Data6<T, A, B, C, D, E>
    ): MutableObservable<OUT> =
            TwoWayMappedObservable(
                    { mapping(value, otherA.value, otherB.value, otherC.value, otherD.value, otherE.value) },
                    {
                        reverseMapping(it).let { (t, a, b, c, d, e) ->
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




