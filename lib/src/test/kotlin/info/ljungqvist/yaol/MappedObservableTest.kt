package info.ljungqvist.yaol

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MappedObservableTest {

    @Test
    fun `onChange should not be run until the mapped value changes`() {

        val o1 = mutableObservable(false)
        val o2 = mutableObservable(false)
        val o3 = o1.join(o2) { v1, v2 -> v1 && v2 }

        var onChangeRun = false

        // keep [sub] to avoid GC
        @Suppress("UNUSED_VARIABLE")
        val sub = o3.onChange {
            onChangeRun = true
        }

        assertFalse(onChangeRun)

        o1.value = true

        assertFalse(onChangeRun)

        o2.value = true

        assertTrue(onChangeRun)
        onChangeRun = false

        o1.value = false

        assertTrue(onChangeRun)

    }

}
