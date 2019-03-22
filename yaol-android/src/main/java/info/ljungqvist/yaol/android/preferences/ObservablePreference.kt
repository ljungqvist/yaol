package info.ljungqvist.yaol.android.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import info.ljungqvist.yaol.ObservableImpl
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors


internal class ObservablePreference<T>(
        private val preferences: () -> SharedPreferences,
        private val key: String,
        get: SharedPreferences.(String, T) -> T,
        private val set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
        default: T
) : ObservableImpl<T>(), MutableObservable<T> {

    private val latch = CountDownLatch(1)
    private var _value: T? = null

    override var value: T
        get() = run {
            latch.await()
            @Suppress("UNCHECKED_CAST")
            _value as T
        }
        @SuppressLint("ApplySharedPref")
        set(value) = synchronized(this) {
            latch.await()
            val update = value != _value
            _value = value
            if (update) {
                notifyChange()
                writeExecutor.submit {
                    preferences().edit().set(key, this.value).commit()
                }
            }
        }

    init {
        readExecutor.submit {
            _value = preferences().get(key, default)
            latch.countDown()
        }
    }

}

private val readExecutor = Executors.newCachedThreadPool()
private val writeExecutor = Executors.newSingleThreadExecutor()
