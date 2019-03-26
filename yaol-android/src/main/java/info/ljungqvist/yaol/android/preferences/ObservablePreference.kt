package info.ljungqvist.yaol.android.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import info.ljungqvist.yaol.ObservableImpl
import java.util.concurrent.Executors
import java.util.concurrent.Future


internal class ObservablePreference<T>(
        private val preferences: () -> SharedPreferences,
        private val key: String,
        get: SharedPreferences.(String, T) -> T,
        private val set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
        default: () -> T
) : ObservableImpl<T>(), MutableObservable<T> {

    private var _value: T? = null

    private val future: Future<*> = readExecutor.submit {
        _value = preferences().get(key, default())
    }

    override var value: T
        get() = run {
            future.get()
            @Suppress("UNCHECKED_CAST")
            _value as T
        }
        @SuppressLint("ApplySharedPref")
        set(value) = synchronized(this) {
            future.get()
            val update = value != _value
            _value = value
            if (update) {
                notifyChange()
                writeExecutor.submit {
                    preferences().edit().set(key, this.value).commit()
                }
            }
        }

}

private val readExecutor = Executors.newCachedThreadPool()
private val writeExecutor = Executors.newSingleThreadExecutor()
