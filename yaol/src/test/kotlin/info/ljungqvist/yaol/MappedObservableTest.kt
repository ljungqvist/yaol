package info.ljungqvist.yaol

import org.junit.Assert
import org.junit.Test

class MappedObservableTest {

    @Test
    fun `onChange should not be run until the mapped value changes`() {

        val o1 = mutableObservable(false)
        val o2 = mutableObservable(false)
        val o3 = o1.join(o2){ v1, v2 -> v1 && v2 }

        var onChangeRun = false

        val sub = o3.onChange {
            onChangeRun = true
        }

        Assert.assertFalse(onChangeRun)

        o1.value = true

        Assert.assertFalse(onChangeRun)

        o2.value = true

        Assert.assertTrue(onChangeRun)
        onChangeRun = false

        o1.value = false

        Assert.assertTrue(onChangeRun)

    }

}