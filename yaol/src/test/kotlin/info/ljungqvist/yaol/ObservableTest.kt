package info.ljungqvist.yaol

import org.junit.Assert
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@RunWith(JUnitPlatform::class)
class ObservableTest : Spek({

    describe("Observable") {

        context("an Observable") {

            val observable = mutableObservable("")

            it("should be subscribable and unsubscribable") {

                observable.value = "one"
                Assert.assertEquals("one", observable.value)
                val ref = AtomicReference<String>("none")

                observable.value = "two"
                var sub = observable.onChange { ref.set(it) }
                Assert.assertEquals("none", ref.get())

                observable.value = "three"
                Assert.assertEquals("three", ref.get())

                sub.close()
                observable.value = "four"
                Assert.assertEquals("three", ref.get())

                sub = observable.runAndOnChange { ref.set(it) }
                Assert.assertEquals("four", ref.get())

                observable.value = "five"
                Assert.assertEquals("five", ref.get())

                sub.close()
                observable.value = "six"
                Assert.assertEquals("five", ref.get())

                observable.runAndOnChangeUntilTrue {
                    ref.set(it)
                    it.length >= 6
                }
                Assert.assertEquals("six", ref.get())

                observable.value = "seven"
                Assert.assertEquals("seven", ref.get())

                observable.value = "eight"
                Assert.assertEquals("eight", ref.get())

                observable.value = "nine"
                Assert.assertEquals("nine", ref.get())

                observable.value = "ten"
                Assert.assertEquals("ten", ref.get())

                observable.value = "eleven"
                Assert.assertEquals("eleven", ref.get())

                observable.value = "twelve"
                Assert.assertEquals("eleven", ref.get())

                observable.runAndOnChangeUntilTrue {
                    ref.set(it)
                    it.length >= 6
                }
                Assert.assertEquals("twelve", ref.get())

                observable.value = "thirteen"
                Assert.assertEquals("twelve", ref.get())

                observable.onChangeUntilTrue {
                    ref.set(it)
                    it.length >= 6
                }
                Assert.assertEquals("twelve", ref.get())

                observable.value = "fourteen"
                Assert.assertEquals("fourteen", ref.get())

                observable.value = "fifteen"
                Assert.assertEquals("fourteen", ref.get())


            }

            it("an Observable must be unsubscribable in OnChange") {

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

                Assert.assertArrayEquals(intArrayOf(-1, -1, -1, -1, -1), arr)

                o.value = 0
                Assert.assertArrayEquals(intArrayOf(0, 0, 0, 0, 0), arr)

                o.value = 1
                Assert.assertArrayEquals(intArrayOf(0, 1, 1, 1, 1), arr)

                o.value = 2
                Assert.assertArrayEquals(intArrayOf(0, 1, 2, 2, 2), arr)

                o.value = 3
                Assert.assertArrayEquals(intArrayOf(0, 1, 2, 3, 3), arr)

                o.value = 4
                Assert.assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), arr)

                o.value = 5
                Assert.assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), arr)

            }

            it("should be mappable") {

                observable.value = "test"

                val ref1 = AtomicReference<String>("")
                val ref2 = AtomicInteger(-1)

                val mappedObservable = observable.map { it.length }

                val s1 = observable.onChange { ref1.set(observable.value) }
                val s2 = mappedObservable.onChange { ref2.set(mappedObservable.value) }

                Assert.assertEquals("", ref1.get())
                Assert.assertEquals(-1, ref2.get())
                Assert.assertEquals("test", observable.value)
                Assert.assertEquals(4, mappedObservable.value)

                observable.value = "test2"

                Assert.assertEquals("test2", ref1.get())
                Assert.assertEquals(5, ref2.get())
                Assert.assertEquals("test2", observable.value)
                Assert.assertEquals(5, mappedObservable.value)

                ref1.set("")
                ref2.set(-1)

                Assert.assertEquals("", ref1.get())
                Assert.assertEquals(-1, ref2.get())
                Assert.assertEquals("test2", observable.value)
                Assert.assertEquals(5, mappedObservable.value)

                observable.value = "test3"

                Assert.assertEquals("test3", ref1.get())
                Assert.assertEquals(-1, ref2.get()) // assert not called
                Assert.assertEquals("test3", observable.value)
                Assert.assertEquals(5, mappedObservable.value)

                s1.close()
                s2.close()

            }


            it("should be mappable multiple times") {

                observable.value = ""
                val mapped1 = observable.map { "$it one" }
                val mapped2 = mapped1.map { "$it two" }
                val mapped3 = mapped2.map { "$it three" }

                Assert.assertEquals("", observable.value)
                Assert.assertEquals(" one", mapped1.value)
                Assert.assertEquals(" one two", mapped2.value)
                Assert.assertEquals(" one two three", mapped3.value)

                observable.value = "zero"

                Assert.assertEquals("zero", observable.value)
                Assert.assertEquals("zero one", mapped1.value)
                Assert.assertEquals("zero one two", mapped2.value)
                Assert.assertEquals("zero one two three", mapped3.value)

            }

            it("nullable should be mappable multiple times") {

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

                Assert.assertNull(mapped0.value)
                Assert.assertNull(mapped1.value)
                Assert.assertNull(mapped2.value)
                Assert.assertNull(mapped3.value)
                Assert.assertEquals("---nothing---", res1)
                Assert.assertEquals("---nothing---", res2)
                Assert.assertEquals("---nothing---", res3)

                mapped0.value = "zero"
                Assert.assertEquals("zero", mapped0.value)
                Assert.assertEquals("zero one", mapped1.value)
                Assert.assertEquals("zero one two", mapped2.value)
                Assert.assertEquals("zero one two three", mapped3.value)
                Assert.assertEquals("zero one", res1)
                Assert.assertEquals("zero one two", res2)
                Assert.assertEquals("zero one two three", res3)

                s1.close()
                s2.close()
                s3.close()

            }

        }

        context("two Observables") {

            val observable1 = mutableObservable("")
            val observable2 = mutableObservable(0)

            it("should be joinable") {

                observable1.value = "test"
                observable2.value = 5

                val ref1 = AtomicReference<String>("")
                val ref2 = AtomicInteger(-1)
                val refJoined = AtomicReference<String>("")
                val refJoined2 = AtomicInteger(-1)

                val joinedObservable = observable1.join(observable2) { string, int -> "$string - $int" }
                val joinedObservable2 = observable1.join(observable2) { string, int -> string.length + int }

                val s1 = observable1.onChange { ref1.set(observable1.value) }
                val s2 = observable2.onChange { ref2.set(observable2.value) }
                val s3 = joinedObservable.onChange { refJoined.set(joinedObservable.value) }
                val s4 = joinedObservable2.onChange { refJoined2.set(joinedObservable2.value) }

                Assert.assertEquals("", ref1.get())
                Assert.assertEquals(-1, ref2.get())
                Assert.assertEquals("", refJoined.get())
                Assert.assertEquals(-1, refJoined2.get())
                Assert.assertEquals("test", observable1.value)
                Assert.assertEquals(5, observable2.value)
                Assert.assertEquals("test - 5", joinedObservable.value)
                Assert.assertEquals(9, joinedObservable2.value)

                observable1.value = "test2"

                Assert.assertEquals("test2", ref1.get())
                Assert.assertEquals(-1, ref2.get())
                Assert.assertEquals("test2 - 5", refJoined.get())
                Assert.assertEquals(10, refJoined2.get())
                Assert.assertEquals("test2", observable1.value)
                Assert.assertEquals(5, observable2.value)
                Assert.assertEquals("test2 - 5", joinedObservable.value)
                Assert.assertEquals(10, joinedObservable2.value)

                observable2.value = 12

                Assert.assertEquals("test2", ref1.get())
                Assert.assertEquals(12, ref2.get())
                Assert.assertEquals("test2 - 12", refJoined.get())
                Assert.assertEquals(17, refJoined2.get())
                Assert.assertEquals("test2", observable1.value)
                Assert.assertEquals(12, observable2.value)
                Assert.assertEquals("test2 - 12", joinedObservable.value)
                Assert.assertEquals(17, joinedObservable2.value)

                refJoined2.set(-1)

                Assert.assertEquals("test2", ref1.get())
                Assert.assertEquals(12, ref2.get())
                Assert.assertEquals("test2 - 12", refJoined.get())
                Assert.assertEquals(-1, refJoined2.get())
                Assert.assertEquals("test2", observable1.value)
                Assert.assertEquals(12, observable2.value)
                Assert.assertEquals("test2 - 12", joinedObservable.value)
                Assert.assertEquals(17, joinedObservable2.value)

                observable1.value = "2test"

                Assert.assertEquals("2test", ref1.get())
                Assert.assertEquals(12, ref2.get())
                Assert.assertEquals("2test - 12", refJoined.get())
                Assert.assertEquals(-1, refJoined2.get()) // assert not called
                Assert.assertEquals("2test", observable1.value)
                Assert.assertEquals(12, observable2.value)
                Assert.assertEquals("2test - 12", joinedObservable.value)
                Assert.assertEquals(17, joinedObservable2.value)

                s1.close()
                s2.close()
                s3.close()
                s4.close()

            }

        }

        context("an Observable mappable to another Observable") {

            val observable = mutableObservable(0)
            val observablePositive = mutableObservable("it is true")
            val observableNegative = mutableObservable("that is not true")

            it("should be flat-mappable") {

                val ref = AtomicReference<Int?>(null)
                val refPositive = AtomicReference<String?>(null)
                val refNegative = AtomicReference<String?>(null)
                val refMapped = AtomicReference<String?>(null)

                val s1 = observable.onChange { ref.set(observable.value) }
                val s2 = observablePositive.onChange { refPositive.set(observablePositive.value) }
                val s3 = observableNegative.onChange { refNegative.set(observableNegative.value) }

                Assert.assertEquals(0, observable.value)
                Assert.assertEquals("it is true", observablePositive.value)
                Assert.assertEquals("that is not true", observableNegative.value)
                Assert.assertNull(ref.get())
                Assert.assertNull(refPositive.get())
                Assert.assertNull(refNegative.get())
                Assert.assertNull(refMapped.get())

                val mappedObservable = observable.flatMap {
                    when {
                        it > 0 -> observablePositive
                        else -> observableNegative
                    }
                }

                val s4 = mappedObservable.onChange { refMapped.set(mappedObservable.value) }

                Assert.assertEquals("that is not true", mappedObservable.value)
                Assert.assertNull(refMapped.get())

                observablePositive.value = "it is almost true"

                Assert.assertEquals(0, observable.value)
                Assert.assertEquals("it is almost true", observablePositive.value)
                Assert.assertEquals("that is not true", observableNegative.value)
                Assert.assertEquals("that is not true", mappedObservable.value)
                Assert.assertNull(ref.get())
                Assert.assertEquals("it is almost true", refPositive.get())
                Assert.assertNull(refNegative.get())
                Assert.assertNull(refMapped.get())

                refPositive.set(null)

                observableNegative.value = "it is not really true"

                Assert.assertEquals(0, observable.value)
                Assert.assertEquals("it is almost true", observablePositive.value)
                Assert.assertEquals("it is not really true", observableNegative.value)
                Assert.assertEquals("it is not really true", mappedObservable.value)
                Assert.assertNull(ref.get())
                Assert.assertNull(refPositive.get())
                Assert.assertEquals("it is not really true", refNegative.get())
                Assert.assertEquals("it is not really true", refMapped.get())

                refNegative.set(null)
                refMapped.set(null)

                observable.value = 1

                Assert.assertEquals(1, observable.value)
                Assert.assertEquals("it is almost true", observablePositive.value)
                Assert.assertEquals("it is not really true", observableNegative.value)
                Assert.assertEquals("it is almost true", mappedObservable.value)
                Assert.assertEquals(1, ref.get())
                Assert.assertNull(refPositive.get())
                Assert.assertNull(refNegative.get())
                Assert.assertEquals("it is almost true", refMapped.get())

                ref.set(null)
                refMapped.set(null)

                observable.value = 1

                Assert.assertEquals(1, observable.value)
                Assert.assertEquals("it is almost true", observablePositive.value)
                Assert.assertEquals("it is not really true", observableNegative.value)
                Assert.assertEquals("it is almost true", mappedObservable.value)
                Assert.assertNull(ref.get())
                Assert.assertNull(refPositive.get())
                Assert.assertNull(refNegative.get())
                Assert.assertNull(refMapped.get())

                observable.value = 2

                Assert.assertEquals(2, observable.value)
                Assert.assertEquals("it is almost true", observablePositive.value)
                Assert.assertEquals("it is not really true", observableNegative.value)
                Assert.assertEquals("it is almost true", mappedObservable.value)
                Assert.assertEquals(2, ref.get())
                Assert.assertNull(refPositive.get())
                Assert.assertNull(refNegative.get())
                Assert.assertNull(refMapped.get())

                ref.set(null)

                observableNegative.value = "it is not yet true"

                Assert.assertEquals(2, observable.value)
                Assert.assertEquals("it is almost true", observablePositive.value)
                Assert.assertEquals("it is not yet true", observableNegative.value)
                Assert.assertEquals("it is almost true", mappedObservable.value)
                Assert.assertNull(ref.get())
                Assert.assertNull(refPositive.get())
                Assert.assertEquals("it is not yet true", refNegative.get())
                Assert.assertNull(refMapped.get())

                refNegative.set(null)

                observablePositive.value = "it is still true"

                Assert.assertEquals(2, observable.value)
                Assert.assertEquals("it is still true", observablePositive.value)
                Assert.assertEquals("it is not yet true", observableNegative.value)
                Assert.assertEquals("it is still true", mappedObservable.value)
                Assert.assertNull(ref.get())
                Assert.assertEquals("it is still true", refPositive.get())
                Assert.assertNull(refNegative.get())
                Assert.assertEquals("it is still true", refMapped.get())

                s1.close()
                s2.close()
                s3.close()
                s4.close()

            }

            it("should be flat-mappable and unsubscribable") {

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

                Assert.assertEquals("1", ref1.get())
                Assert.assertEquals("1", ref2.get())
                Assert.assertEquals("1", oMapped.value)

                o3.value = true

                Assert.assertEquals("one", ref1.get())
                Assert.assertEquals("one", ref2.get())
                Assert.assertEquals("one", oMapped.value)

                o1.value = "two"

                Assert.assertEquals("two", ref1.get())
                Assert.assertEquals("two", ref2.get())
                Assert.assertEquals("two", oMapped.value)

                s1.close()
                o1.value = "three"

                Assert.assertEquals("two", ref1.get())
                Assert.assertEquals("three", ref2.get())
                Assert.assertEquals("three", oMapped.value)

                s2.close()

            }

        }

        context("Obervable delegates") {

            class TestClass(init: String) {
                val mutableObservable = mutableObservable(init)
                var mutable by mutableObservable
                val immutableObservable = mutableObservable.map { "$it $it" }
                val immutable by immutableObservable
            }

            it("delegates properties should have the same value as the observable") {

                val test = TestClass("one")
                Assert.assertEquals("one", test.mutableObservable.value)
                Assert.assertEquals("one", test.mutable)
                Assert.assertEquals("one one", test.immutableObservable.value)
                Assert.assertEquals("one one", test.immutable)

                test.mutableObservable.value = "two"
                Assert.assertEquals("two", test.mutableObservable.value)
                Assert.assertEquals("two", test.mutable)
                Assert.assertEquals("two two", test.immutableObservable.value)
                Assert.assertEquals("two two", test.immutable)

                test.mutable = "three"
                Assert.assertEquals("three", test.mutableObservable.value)
                Assert.assertEquals("three", test.mutable)
                Assert.assertEquals("three three", test.immutableObservable.value)
                Assert.assertEquals("three three", test.immutable)

            }

        }

        context("List of observables") {

            it("should be joined to an observable list") {

                val observableList: List<MutableObservable<String>> = listOf(
                        mutableObservable("I"),
                        mutableObservable("am"),
                        mutableObservable("here.")
                )
                val listObservable = observableList.join { it.joinToString(" ") }

                Assert.assertEquals("I am here.", listObservable.value)

                observableList[2].value = "there."
                Assert.assertEquals("I am there.", listObservable.value)

                observableList[0].value = "You"
                observableList[1].value = "are"
                Assert.assertEquals("You are there.", listObservable.value)

            }

            class ListObservableHolder {
                val listObservable: MutableObservable<List<Observable<String>>> = mutableObservable(emptyList())
                var list by listObservable
            }

            it("should be quick to join") {

                val holder = ListObservableHolder()
                val observable = holder.listObservable
                        .flatMap { listObservable ->
                            listObservable.join { it.joinToString(", ") }
                        }
                        .map { " - $it" }
                var ref: String? = null
                val s = observable.runAndOnChange { ref = observable.value }

                Assert.assertEquals(" - ", ref)

                holder.list += immutableObservable("Hello")
                Assert.assertEquals(" - Hello", ref)

                holder.list += immutableObservable("World")
                Assert.assertEquals(" - Hello, World", ref)

                holder.list += immutableObservable("one")
                Assert.assertEquals(" - Hello, World, one", ref)

                holder.list += immutableObservable("two")
                Assert.assertEquals(" - Hello, World, one, two", ref)

                var string = " - Hello, World, one, two"
                (0..1000).forEach {
                    string += ", $it"
                    holder.list += immutableObservable("$it")
                    Assert.assertEquals(string, ref)
                }

                s.close()

            }

        }

    }

    describe("MutableObservable") {

        it("should be mappable to another MutableObservable") {
            val o1 = mutableObservable("test")
            val o2 = o1.twoWayMap({ it.toCharArray() }, { it.joinToString("") })

            Assert.assertEquals(o1.value, "test")
            Assert.assertArrayEquals(o2.value, charArrayOf('t', 'e', 's', 't'))

            o1.value = "one"

            Assert.assertEquals(o1.value, "one")
            Assert.assertArrayEquals(o2.value, charArrayOf('o', 'n', 'e'))

            o2.value = charArrayOf('t', 'w', 'o')

            Assert.assertEquals(o1.value, "two")
            Assert.assertArrayEquals(o2.value, charArrayOf('t', 'w', 'o'))

        }

        it("MutableObservable should be two-way joinable") {
            val o1 = mutableObservable("one")
            val o2 = mutableObservable(1)
            val o3 = mutableObservable('a')

            val joined = o1.twoWayJoin(o2, o3, ::Triple) { data(it.first, it.second, it.third) }

            Assert.assertEquals("one", o1.value)
            Assert.assertEquals(1, o2.value)
            Assert.assertEquals('a', o3.value)
            Assert.assertEquals(Triple("one", 1, 'a'), joined.value)

            o1.value = "two"

            Assert.assertEquals("two", o1.value)
            Assert.assertEquals(1, o2.value)
            Assert.assertEquals('a', o3.value)
            Assert.assertEquals(Triple("two", 1, 'a'), joined.value)

            o2.value = 2
            o3.value = '2'

            Assert.assertEquals("two", o1.value)
            Assert.assertEquals(2, o2.value)
            Assert.assertEquals('2', o3.value)
            Assert.assertEquals(Triple("two", 2, '2'), joined.value)

            joined.value = Triple("three", 4, 'e')

            Assert.assertEquals("three", o1.value)
            Assert.assertEquals(4, o2.value)
            Assert.assertEquals('e', o3.value)
            Assert.assertEquals(Triple("three", 4, 'e'), joined.value)

        }

        it("a list of mutable observables should be joinable") {

            val o1 = mutableObservable(1.0)
            val o2 = mutableObservable(2.0)
            val o3 = mutableObservable(3.0)
            val oList = listOf(o1, o2, o3)

            val joined = oList.twoWayJoin(
                    { list -> list.reversed().map { it * 2.0 } },
                    { list -> list.map { it / 2.0 }.reversed() }
            )

            val d = 0.000001

            Assert.assertEquals(1.0, o1.value, d)
            Assert.assertEquals(2.0, o2.value, d)
            Assert.assertEquals(3.0, o3.value, d)
            Assert.assertEquals(listOf(6.0, 4.0, 2.0), joined.value)

            o1.value = 0.1

            Assert.assertEquals(0.1, o1.value, d)
            Assert.assertEquals(2.0, o2.value, d)
            Assert.assertEquals(3.0, o3.value, d)
            Assert.assertEquals(listOf(6.0, 4.0, 0.2), joined.value)

            o2.value = 0.2
            o3.value = 0.3

            Assert.assertEquals(0.1, o1.value, d)
            Assert.assertEquals(0.2, o2.value, d)
            Assert.assertEquals(0.3, o3.value, d)
            Assert.assertEquals(listOf(0.6, 0.4, 0.2), joined.value)

            joined.value = listOf(1.2, 3.4, 5.6)

            Assert.assertEquals(2.8, o1.value, d)
            Assert.assertEquals(1.7, o2.value, d)
            Assert.assertEquals(0.6, o3.value, d)
            Assert.assertEquals(listOf(1.2, 3.4, 5.6), joined.value)

        }

    }

    describe("Observable being set while 'runAndOnChange' is executing its first run") {

        it("should execute the callback for the new value") {

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

            Assert.assertEquals("12", result.get())

            subscription?.close()

        }

    }

})
