package info.ljungqvist.yaol

import org.junit.Assert
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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

                sub.unsubscribe()
                observable.value = "four"
                Assert.assertEquals("three", ref.get())

                sub = observable.runAndOnChange { ref.set(it) }
                Assert.assertEquals("four", ref.get())

                observable.value = "five"
                Assert.assertEquals("five", ref.get())

                sub.unsubscribe()
                observable.value = "six"
                Assert.assertEquals("five", ref.get())

                observable.runAndOnChangeUnitTrue {
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

                observable.runAndOnChangeUnitTrue {
                    ref.set(it)
                    it.length >= 6
                }
                Assert.assertEquals("twelve", ref.get())

                observable.value = "thirteen"
                Assert.assertEquals("twelve", ref.get())


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

                s1.unsubscribe()
                s2.unsubscribe()

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

                s1.unsubscribe()
                s2.unsubscribe()
                s3.unsubscribe()
                s4.unsubscribe()

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

                var s4 = mappedObservable.onChange { refMapped.set(mappedObservable.value) }

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

                s1.unsubscribe()
                s2.unsubscribe()
                s3.unsubscribe()
                s4.unsubscribe()

            }

        }

    }
})