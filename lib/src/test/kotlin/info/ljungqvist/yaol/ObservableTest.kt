package info.ljungqvist.yaol

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ObservableTest{

    @Test
    fun `an Observable should be subscribable and unsubscribable`() {
        val observable = mutableObservable("")

        observable.value = "one"
        assertEquals("one", observable.value)
        val ref = AtomicReference("none")

        observable.value = "two"
        var sub = observable.onChange { ref.set(it) }
        assertEquals("none", ref.get())

        observable.value = "three"
        assertEquals("three", ref.get())

        sub.close()
        observable.value = "four"
        assertEquals("three", ref.get())

        sub = observable.runAndOnChange { ref.set(it) }
        assertEquals("four", ref.get())

        observable.value = "five"
        assertEquals("five", ref.get())

        sub.close()
        observable.value = "six"
        assertEquals("five", ref.get())

        observable.runAndOnChangeUntilTrue {
            ref.set(it)
            it.length >= 6
        }
        assertEquals("six", ref.get())

        observable.value = "seven"
        assertEquals("seven", ref.get())

        observable.value = "eight"
        assertEquals("eight", ref.get())

        observable.value = "nine"
        assertEquals("nine", ref.get())

        observable.value = "ten"
        assertEquals("ten", ref.get())

        observable.value = "eleven"
        assertEquals("eleven", ref.get())

        observable.value = "twelve"
        assertEquals("eleven", ref.get())

        observable.runAndOnChangeUntilTrue {
            ref.set(it)
            it.length >= 6
        }
        assertEquals("twelve", ref.get())

        observable.value = "thirteen"
        assertEquals("twelve", ref.get())

        observable.onChangeUntilTrue {
            ref.set(it)
            it.length >= 6
        }
        assertEquals("twelve", ref.get())

        observable.value = "fourteen"
        assertEquals("fourteen", ref.get())

        observable.value = "fifteen"
        assertEquals("fourteen", ref.get())
    }

    @Test fun `an Observable must be unsubscribable in OnChange`() {

        val o = mutableObservable(-1)

        val arr = intArrayOf(-1, -1, -1, -1, -1)

        fun addChangeUntilTrue(value: Int) {
            o.onChangeUntilTrue {
                arr[value] = it
                it == value
            }
        }

        addChangeUntilTrue(3)
        addChangeUntilTrue(1)
        addChangeUntilTrue(4)
        addChangeUntilTrue(0)
        addChangeUntilTrue(2)

        assertContentEquals(intArrayOf(-1, -1, -1, -1, -1), arr)

        o.value = 0
        assertContentEquals(intArrayOf(0, 0, 0, 0, 0), arr)

        o.value = 1
        assertContentEquals(intArrayOf(0, 1, 1, 1, 1), arr)

        o.value = 2
        assertContentEquals(intArrayOf(0, 1, 2, 2, 2), arr)

        o.value = 3
        assertContentEquals(intArrayOf(0, 1, 2, 3, 3), arr)

        o.value = 4
        assertContentEquals(intArrayOf(0, 1, 2, 3, 4), arr)

        o.value = 5
        assertContentEquals(intArrayOf(0, 1, 2, 3, 4), arr)

    }

    @Test fun `an Observable should be mappable`() {
        val observable = mutableObservable("")

        observable.value = "test"

        val ref1 = AtomicReference("")
        val ref2 = AtomicInteger(-1)

        val mappedObservable = observable.map { it.length }

        val s1 = observable.onChange { ref1.set(observable.value) }
        val s2 = mappedObservable.onChange { ref2.set(mappedObservable.value) }

        assertEquals("", ref1.get())
        assertEquals(-1, ref2.get())
        assertEquals("test", observable.value)
        assertEquals(4, mappedObservable.value)

        observable.value = "test2"

        assertEquals("test2", ref1.get())
        assertEquals(5, ref2.get())
        assertEquals("test2", observable.value)
        assertEquals(5, mappedObservable.value)

        ref1.set("")
        ref2.set(-1)

        assertEquals("", ref1.get())
        assertEquals(-1, ref2.get())
        assertEquals("test2", observable.value)
        assertEquals(5, mappedObservable.value)

        observable.value = "test3"

        assertEquals("test3", ref1.get())
        assertEquals(-1, ref2.get()) // assert not called
        assertEquals("test3", observable.value)
        assertEquals(5, mappedObservable.value)

        s1.close()
        s2.close()
    }

    @Test fun `an Observable should be mappable multiple times`() {
        val observable = mutableObservable("")

        observable.value = ""
        val mapped1 = observable.map { "$it one" }
        val mapped2 = mapped1.map { "$it two" }
        val mapped3 = mapped2.map { "$it three" }

        assertEquals("", observable.value)
        assertEquals(" one", mapped1.value)
        assertEquals(" one two", mapped2.value)
        assertEquals(" one two three", mapped3.value)

        observable.value = "zero"

        assertEquals("zero", observable.value)
        assertEquals("zero one", mapped1.value)
        assertEquals("zero one two", mapped2.value)
        assertEquals("zero one two three", mapped3.value)

    }

    @Test fun `a nullable Observable should be mappable multiple times`() {

        val mapped0: MutableObservable<String?> = mutableObservable(null)
        val mapped1 = mapped0.map { value -> value?.let { "$it one" } }
        val mapped2 = mapped1.map { value -> value?.let { "$it two" } }
        val mapped3 = mapped2.map { value -> value?.let { "$it three" } }
        var res1: String? = "---nothing---"
        var res2: String? = "---nothing---"
        var res3: String? = "---nothing---"
        val s1 = mapped1.onChange { res1 = it }
        val s2 = mapped2.onChange { res2 = it }
        val s3 = mapped3.onChange { res3 = it }

        assertNull(mapped0.value)
        assertNull(mapped1.value)
        assertNull(mapped2.value)
        assertNull(mapped3.value)
        assertEquals("---nothing---", res1)
        assertEquals("---nothing---", res2)
        assertEquals("---nothing---", res3)

        mapped0.value = "zero"
        assertEquals("zero", mapped0.value)
        assertEquals("zero one", mapped1.value)
        assertEquals("zero one two", mapped2.value)
        assertEquals("zero one two three", mapped3.value)
        assertEquals("zero one", res1)
        assertEquals("zero one two", res2)
        assertEquals("zero one two three", res3)

        s1.close()
        s2.close()
        s3.close()

    }

    @Test fun `two Observables should be joinable`() {
        val observable1 = mutableObservable("")
        val observable2 = mutableObservable(0)

        observable1.value = "test"
        observable2.value = 5

        val ref1 = AtomicReference("")
        val ref2 = AtomicInteger(-1)
        val refJoined = AtomicReference("")
        val refJoined2 = AtomicInteger(-1)

        val joinedObservable = observable1.join(observable2) { string, int -> "$string - $int" }
        val joinedObservable2 = observable1.join(observable2) { string, int -> string.length + int }

        val s1 = observable1.onChange { ref1.set(observable1.value) }
        val s2 = observable2.onChange { ref2.set(observable2.value) }
        val s3 = joinedObservable.onChange { refJoined.set(joinedObservable.value) }
        val s4 = joinedObservable2.onChange { refJoined2.set(joinedObservable2.value) }

        assertEquals("", ref1.get())
        assertEquals(-1, ref2.get())
        assertEquals("", refJoined.get())
        assertEquals(-1, refJoined2.get())
        assertEquals("test", observable1.value)
        assertEquals(5, observable2.value)
        assertEquals("test - 5", joinedObservable.value)
        assertEquals(9, joinedObservable2.value)

        observable1.value = "test2"

        assertEquals("test2", ref1.get())
        assertEquals(-1, ref2.get())
        assertEquals("test2 - 5", refJoined.get())
        assertEquals(10, refJoined2.get())
        assertEquals("test2", observable1.value)
        assertEquals(5, observable2.value)
        assertEquals("test2 - 5", joinedObservable.value)
        assertEquals(10, joinedObservable2.value)

        observable2.value = 12

        assertEquals("test2", ref1.get())
        assertEquals(12, ref2.get())
        assertEquals("test2 - 12", refJoined.get())
        assertEquals(17, refJoined2.get())
        assertEquals("test2", observable1.value)
        assertEquals(12, observable2.value)
        assertEquals("test2 - 12", joinedObservable.value)
        assertEquals(17, joinedObservable2.value)

        refJoined2.set(-1)

        assertEquals("test2", ref1.get())
        assertEquals(12, ref2.get())
        assertEquals("test2 - 12", refJoined.get())
        assertEquals(-1, refJoined2.get())
        assertEquals("test2", observable1.value)
        assertEquals(12, observable2.value)
        assertEquals("test2 - 12", joinedObservable.value)
        assertEquals(17, joinedObservable2.value)

        observable1.value = "2test"

        assertEquals("2test", ref1.get())
        assertEquals(12, ref2.get())
        assertEquals("2test - 12", refJoined.get())
        assertEquals(-1, refJoined2.get()) // assert not called
        assertEquals("2test", observable1.value)
        assertEquals(12, observable2.value)
        assertEquals("2test - 12", joinedObservable.value)
        assertEquals(17, joinedObservable2.value)

        s1.close()
        s2.close()
        s3.close()
        s4.close()

    }

    @Test fun `an Observable mappable to another Observable should be flat-mappable`(){

        val observable = mutableObservable(0)
        val observablePositive = mutableObservable("it is true")
        val observableNegative = mutableObservable("that is not true")

        val ref = AtomicReference<Int?>(null)
        val refPositive = AtomicReference<String?>(null)
        val refNegative = AtomicReference<String?>(null)
        val refMapped = AtomicReference<String?>(null)

        val s1 = observable.onChange { ref.set(observable.value) }
        val s2 = observablePositive.onChange { refPositive.set(observablePositive.value) }
        val s3 = observableNegative.onChange { refNegative.set(observableNegative.value) }

        assertEquals(0, observable.value)
        assertEquals("it is true", observablePositive.value)
        assertEquals("that is not true", observableNegative.value)
        assertNull(ref.get())
        assertNull(refPositive.get())
        assertNull(refNegative.get())
        assertNull(refMapped.get())

        val mappedObservable = observable.flatMap {
            when {
                it > 0 -> observablePositive
                else -> observableNegative
            }
        }

        val s4 = mappedObservable.onChange { refMapped.set(mappedObservable.value) }

        assertEquals("that is not true", mappedObservable.value)
        assertNull(refMapped.get())

        observablePositive.value = "it is almost true"

        assertEquals(0, observable.value)
        assertEquals("it is almost true", observablePositive.value)
        assertEquals("that is not true", observableNegative.value)
        assertEquals("that is not true", mappedObservable.value)
        assertNull(ref.get())
        assertEquals("it is almost true", refPositive.get())
        assertNull(refNegative.get())
        assertNull(refMapped.get())

        refPositive.set(null)

        observableNegative.value = "it is not really true"

        assertEquals(0, observable.value)
        assertEquals("it is almost true", observablePositive.value)
        assertEquals("it is not really true", observableNegative.value)
        assertEquals("it is not really true", mappedObservable.value)
        assertNull(ref.get())
        assertNull(refPositive.get())
        assertEquals("it is not really true", refNegative.get())
        assertEquals("it is not really true", refMapped.get())

        refNegative.set(null)
        refMapped.set(null)

        observable.value = 1

        assertEquals(1, observable.value)
        assertEquals("it is almost true", observablePositive.value)
        assertEquals("it is not really true", observableNegative.value)
        assertEquals("it is almost true", mappedObservable.value)
        assertEquals(1, ref.get())
        assertNull(refPositive.get())
        assertNull(refNegative.get())
        assertEquals("it is almost true", refMapped.get())

        ref.set(null)
        refMapped.set(null)

        observable.value = 1

        assertEquals(1, observable.value)
        assertEquals("it is almost true", observablePositive.value)
        assertEquals("it is not really true", observableNegative.value)
        assertEquals("it is almost true", mappedObservable.value)
        assertNull(ref.get())
        assertNull(refPositive.get())
        assertNull(refNegative.get())
        assertNull(refMapped.get())

        observable.value = 2

        assertEquals(2, observable.value)
        assertEquals("it is almost true", observablePositive.value)
        assertEquals("it is not really true", observableNegative.value)
        assertEquals("it is almost true", mappedObservable.value)
        assertEquals(2, ref.get())
        assertNull(refPositive.get())
        assertNull(refNegative.get())
        assertNull(refMapped.get())

        ref.set(null)

        observableNegative.value = "it is not yet true"

        assertEquals(2, observable.value)
        assertEquals("it is almost true", observablePositive.value)
        assertEquals("it is not yet true", observableNegative.value)
        assertEquals("it is almost true", mappedObservable.value)
        assertNull(ref.get())
        assertNull(refPositive.get())
        assertEquals("it is not yet true", refNegative.get())
        assertNull(refMapped.get())

        refNegative.set(null)

        observablePositive.value = "it is still true"

        assertEquals(2, observable.value)
        assertEquals("it is still true", observablePositive.value)
        assertEquals("it is not yet true", observableNegative.value)
        assertEquals("it is still true", mappedObservable.value)
        assertNull(ref.get())
        assertEquals("it is still true", refPositive.get())
        assertNull(refNegative.get())
        assertEquals("it is still true", refMapped.get())

        s1.close()
        s2.close()
        s3.close()
        s4.close()

    }

    @Test fun `an Observable mappable to another Observable should be flat-mappable to mutable`() {

        val observable = mutableObservable(0)
        val observablePositive = mutableObservable("it is true")
        val observableNegative = mutableObservable("that is not true")

        val ref = AtomicReference<Int?>(null)
        val refPositive = AtomicReference<String?>(null)
        val refNegative = AtomicReference<String?>(null)
        val refMapped = AtomicReference<String?>(null)

        val s1 = observable.onChange { ref.set(observable.value) }
        val s2 = observablePositive.onChange { refPositive.set(observablePositive.value) }
        val s3 = observableNegative.onChange { refNegative.set(observableNegative.value) }

        assertEquals(0, observable.value)
        assertEquals("it is true", observablePositive.value)
        assertEquals("that is not true", observableNegative.value)
        assertNull(ref.get())
        assertNull(refPositive.get())
        assertNull(refNegative.get())
        assertNull(refMapped.get())

        val mappedObservable = observable.flatMapMutable {
            when {
                it > 0 -> observablePositive
                else -> observableNegative
            }
        }

        val s4 = mappedObservable.onChange { refMapped.set(mappedObservable.value) }

        assertEquals("that is not true", mappedObservable.value)
        assertNull(refMapped.get())

        mappedObservable.value = "that is not really true"

        assertEquals(0, observable.value)
        assertEquals("it is true", observablePositive.value)
        assertEquals("that is not really true", observableNegative.value)
        assertEquals("that is not really true", mappedObservable.value)
        assertNull(ref.get())
        assertNull(refPositive.get())
        assertEquals("that is not really true", refNegative.get())
        assertEquals("that is not really true", refMapped.get())

        refNegative.set(null)
        refMapped.set(null)


        observable.value = 1

        assertEquals(1, observable.value)
        assertEquals("it is true", observablePositive.value)
        assertEquals("that is not really true", observableNegative.value)
        assertEquals("it is true", mappedObservable.value)
        assertEquals(1, ref.get())
        assertNull(refPositive.get())
        assertNull(refNegative.get())
        assertEquals("it is true", refMapped.get())

        ref.set(null)
        refMapped.set(null)


        observablePositive.value = "it is quite true"

        assertEquals(1, observable.value)
        assertEquals("it is quite true", observablePositive.value)
        assertEquals("that is not really true", observableNegative.value)
        assertEquals("it is quite true", mappedObservable.value)
        assertNull(ref.get())
        assertEquals("it is quite true", refPositive.get())
        assertNull(refNegative.get())
        assertEquals("it is quite true", refMapped.get())

        refPositive.set(null)
        refMapped.set(null)


        mappedObservable.value = "it is now true"

        assertEquals(1, observable.value)
        assertEquals("it is now true", observablePositive.value)
        assertEquals("that is not really true", observableNegative.value)
        assertEquals("it is now true", mappedObservable.value)
        assertNull(ref.get())
        assertEquals("it is now true", refPositive.get())
        assertNull(refNegative.get())
        assertEquals("it is now true", refMapped.get())

        refPositive.set(null)
        refMapped.set(null)


        s1.close()
        s2.close()
        s3.close()
        s4.close()

    }

    @Test fun `an Observable mappable to another Observable should be flat-mappable and unsubscribable`() {

        val o1 = mutableObservable("one")
        val o2 = mutableObservable(1)
        val o3 = mutableObservable(false)
        val oMapped = o3.flatMap { boolean ->
            if (boolean) {
                o1
            } else {
                o2.map { it.toString() }
            }
        }
        val ref1 = AtomicReference("")
        val ref2 = AtomicReference("")
        val s1 = oMapped.runAndOnChange(ref1::set)
        val s2 = oMapped.runAndOnChange(ref2::set)

        assertEquals("1", ref1.get())
        assertEquals("1", ref2.get())
        assertEquals("1", oMapped.value)

        o3.value = true

        assertEquals("one", ref1.get())
        assertEquals("one", ref2.get())
        assertEquals("one", oMapped.value)

        o1.value = "two"

        assertEquals("two", ref1.get())
        assertEquals("two", ref2.get())
        assertEquals("two", oMapped.value)

        s1.close()
        o1.value = "three"

        assertEquals("two", ref1.get())
        assertEquals("three", ref2.get())
        assertEquals("three", oMapped.value)

        s2.close()

    }

    class TestClass(init: String) {
        val mutableObservable = mutableObservable(init)
        var mutable by mutableObservable
        val immutableObservable = mutableObservable.map { "$it $it" }
        val immutable by immutableObservable
    }

    @Test fun `Obervable delegates properties should have the same value as the observable`() {

        val test = TestClass("one")
        assertEquals("one", test.mutableObservable.value)
        assertEquals("one", test.mutable)
        assertEquals("one one", test.immutableObservable.value)
        assertEquals("one one", test.immutable)

        test.mutableObservable.value = "two"
        assertEquals("two", test.mutableObservable.value)
        assertEquals("two", test.mutable)
        assertEquals("two two", test.immutableObservable.value)
        assertEquals("two two", test.immutable)

        test.mutable = "three"
        assertEquals("three", test.mutableObservable.value)
        assertEquals("three", test.mutable)
        assertEquals("three three", test.immutableObservable.value)
        assertEquals("three three", test.immutable)

    }

    @Test fun `List of observables should be joined to an observable list`() {

        val observableList: List<MutableObservable<String>> = listOf(
            mutableObservable("I"),
            mutableObservable("am"),
            mutableObservable("here.")
        )
        val listObservable = observableList.join { it.joinToString(" ") }

        assertEquals("I am here.", listObservable.value)

        observableList[2].value = "there."
        assertEquals("I am there.", listObservable.value)

        observableList[0].value = "You"
        observableList[1].value = "are"
        assertEquals("You are there.", listObservable.value)

    }

    @Test fun `List of observables should be quick to join`() {
        class ListObservableHolder {
            val listObservable: MutableObservable<List<Observable<String>>> = mutableObservable(emptyList())
            var list by listObservable
        }

        val holder = ListObservableHolder()
        val observable = holder.listObservable
            .flatMap { listObservable ->
                listObservable.join { it.joinToString(", ") }
            }
            .map { " - $it" }
        var ref: String? = null
        val s = observable.runAndOnChange { ref = observable.value }

        assertEquals(" - ", ref)

        holder.list += immutableObservable("Hello")
        assertEquals(" - Hello", ref)

        holder.list += immutableObservable("World")
        assertEquals(" - Hello, World", ref)

        holder.list += immutableObservable("one")
        assertEquals(" - Hello, World, one", ref)

        holder.list += immutableObservable("two")
        assertEquals(" - Hello, World, one, two", ref)

        var string = " - Hello, World, one, two"
        (0..100).forEach {
            string += ", $it"
            holder.list += immutableObservable("$it")
            assertEquals(string, ref)
        }

        s.close()

    }

    @Test fun `MutableObservable should be mappable to another MutableObservable`() {
        val o1 = mutableObservable("test")
        val o2 = o1.twoWayMap({ it.toCharArray() }, { _, chars -> chars.joinToString("") })

        assertEquals(o1.value, "test")
        assertContentEquals(o2.value, charArrayOf('t', 'e', 's', 't'))

        o1.value = "one"

        assertEquals(o1.value, "one")
        assertContentEquals(o2.value, charArrayOf('o', 'n', 'e'))

        o2.value = charArrayOf('t', 'w', 'o')

        assertEquals(o1.value, "two")
        assertContentEquals(o2.value, charArrayOf('t', 'w', 'o'))
    }

    @Test fun `MutableObservable should be mappable to a simpler MutableObservable`() {
        val o1 = mutableObservable("test")
        val o2 = o1.twoWayMap(
            { it.contains('a') },
            { currentValue, mappedValue ->
                if (mappedValue) {
                    currentValue + "a"
                } else {
                    currentValue.replace("a", "")
                }
            }
        )

        assertEquals(o1.value, "test")
        assertEquals(o2.value, false)

        o1.value = "one"

        assertEquals(o1.value, "one")
        assertEquals(o2.value, false)

        o2.value = true

        assertEquals(o1.value, "onea")
        assertEquals(o2.value, true)

        o1.value = "banana"

        assertEquals(o1.value, "banana")
        assertEquals(o2.value, true)

        o2.value = false

        assertEquals(o1.value, "bnn")
        assertEquals(o2.value, false)
    }

    @Test fun `MutableObservable should be two-way joinable`() {
        val o1 = mutableObservable("one")
        val o2 = mutableObservable(1)
        val o3 = mutableObservable('a')

        val joined = o1.twoWayJoin(o2, o3, ::Triple) { _, triple -> data(triple.first, triple.second, triple.third) }

        assertEquals("one", o1.value)
        assertEquals(1, o2.value)
        assertEquals('a', o3.value)
        assertEquals(Triple("one", 1, 'a'), joined.value)

        o1.value = "two"

        assertEquals("two", o1.value)
        assertEquals(1, o2.value)
        assertEquals('a', o3.value)
        assertEquals(Triple("two", 1, 'a'), joined.value)

        o2.value = 2
        o3.value = '2'

        assertEquals("two", o1.value)
        assertEquals(2, o2.value)
        assertEquals('2', o3.value)
        assertEquals(Triple("two", 2, '2'), joined.value)

        joined.value = Triple("three", 4, 'e')

        assertEquals("three", o1.value)
        assertEquals(4, o2.value)
        assertEquals('e', o3.value)
        assertEquals(Triple("three", 4, 'e'), joined.value)
    }

    @Test fun `a list of mutable observables should be joinable`() {

        val o1 = mutableObservable(1.0)
        val o2 = mutableObservable(2.0)
        val o3 = mutableObservable(3.0)
        val oList = listOf(o1, o2, o3)

        val joined = oList.twoWayJoin(
            { list -> list.reversed().map { it * 2.0 } },
            { list -> list.map { it / 2.0 }.reversed() }
        )

        val d = 0.000001

        assertEquals(1.0, o1.value, d)
        assertEquals(2.0, o2.value, d)
        assertEquals(3.0, o3.value, d)
        assertEquals(listOf(6.0, 4.0, 2.0), joined.value)

        o1.value = 0.1

        assertEquals(0.1, o1.value, d)
        assertEquals(2.0, o2.value, d)
        assertEquals(3.0, o3.value, d)
        assertEquals(listOf(6.0, 4.0, 0.2), joined.value)

        o2.value = 0.2
        o3.value = 0.3

        assertEquals(0.1, o1.value, d)
        assertEquals(0.2, o2.value, d)
        assertEquals(0.3, o3.value, d)
        assertEquals(listOf(0.6, 0.4, 0.2), joined.value)

        joined.value = listOf(1.2, 3.4, 5.6)

        assertEquals(2.8, o1.value, d)
        assertEquals(1.7, o2.value, d)
        assertEquals(0.6, o3.value, d)
        assertEquals(listOf(1.2, 3.4, 5.6), joined.value)

    }

    @Test fun `Observable being set while 'runAndOnChange' is executing its first run should execute the callback for the new value`() {
        val result = AtomicReference("")
        val observable = mutableObservable(1)
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)

        var subscription: Subscription? = null

        thread {
            subscription = observable.runAndOnChange {
                latch1.countDown()
                result.accumulateAndGet(it.toString(), String::plus)
                Thread.sleep(10)
            }
            latch2.countDown()
        }

        latch1.await()
        observable.value = 2

        latch2.await()

        assertEquals("12", result.get())

        subscription?.close()
    }

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VARIABLE")
    @Test fun `Adding a mapped observable in the onChange() should not be possible`() {

        val o1 = mutableObservable(0)
        var o2: Observable<String>? = null
        val o3 = o1.map {
            o2 = o1.map { "$it - $it" }
            it.toString()
        }

        var t: IllegalStateException? = null

        try {
            o1.value = 1
        } catch (e: IllegalStateException) {
            t = e
        }

        assertNotNull(t)
        assertEquals("addMappedObservables called from inside notifyChange", t.message)

    }

}
