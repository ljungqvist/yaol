package info.ljungqvist.yaol

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <R, T> lazyWrapper(delegate: () -> ReadOnlyProperty<R, T>): ReadOnlyProperty<R, T> =
        object : ReadOnlyProperty<R, T> {
            override operator fun getValue(thisRef: R, property: KProperty<*>): T = delegate().getValue(thisRef, property)
        }

fun <R, T> lazyMutableWrapper(delegate: () -> ReadWriteProperty<R, T>): ReadWriteProperty<R, T> =
        object : ReadWriteProperty<R, T> {
            override operator fun getValue(thisRef: R, property: KProperty<*>): T = delegate().getValue(thisRef, property)
            override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
                delegate().setValue(thisRef, property, value)
            }
        }
