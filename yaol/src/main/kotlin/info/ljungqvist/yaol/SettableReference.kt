package info.ljungqvist.yaol

sealed class SettableReference<out T> {
    object Unset : SettableReference<Nothing>()
    class Set<out T>(val value: T) : SettableReference<T>()

    inline fun <R> let(ifSet: (T) -> R, ifUnset: () -> R): R =
        when (this) {
            is Set -> ifSet(this.value)
            is Unset -> ifUnset()
        }
}
