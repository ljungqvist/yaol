package info.ljungqvist.yaol

data class Data1<A>(val a: A)
data class Data2<A, B>(val a: A, val b: B)
data class Data3<A, B, C>(val a: A, val b: B, val c: C)
data class Data4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
data class Data5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
data class Data6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)

fun <A> data(a: A) = Data1(a)
fun <A, B> data(a: A, b: B) = Data2(a, b)
fun <A, B, C> data(a: A, b: B, c: C) = Data3(a, b, c)
fun <A, B, C, D> data(a: A, b: B, c: C, d: D) = Data4(a, b, c, d)
fun <A, B, C, D, E> data(a: A, b: B, c: C, d: D, e: E) = Data5(a, b, c, d, e)
fun <A, B, C, D, E, F> data(a: A, b: B, c: C, d: D, e: E, f: F) = Data6(a, b, c, d, e, f)

fun <A, B> Pair<A, B>.toData() = data(first, second)
fun <A, B, C> Triple<A, B, C>.toData() = data(first, second, third)
