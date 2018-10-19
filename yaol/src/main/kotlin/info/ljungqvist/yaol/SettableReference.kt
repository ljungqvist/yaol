package info.ljungqvist.yaol

sealed class SettableReference<out T> {
    object Unset : SettableReference<Nothing>()
    class Set<T>(val value: T) : SettableReference<T>()
}