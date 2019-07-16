package info.ljungqvist.yaol.android.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import info.ljungqvist.yaol.ObservableImpl
import java.util.concurrent.Executors
import java.util.concurrent.Future


@SuppressLint("ApplySharedPref")
internal class ObservablePreference<T>(
        private val preferences: () -> SharedPreferences,
        private val key: String,
        get: SharedPreferences.(String, T) -> T,
        private val set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
        default: (SharedPreferences) -> T
) : ObservableImpl<T>(), MutableObservable<T> {

    private var _value: T? = null

    private var default: T? = null

    private val future: Future<*> = readExecutor.submit {
        val prefs = preferences()
        val defaultValue = default(prefs)
        this.default = defaultValue

        _value =
                if (defaultValue != null && !prefs.contains(key)) {
                    writeExecutor.submit {
                        prefs.edit().set(key, defaultValue).commit()
                    }
                    defaultValue
                } else {
                    prefs.get(key, defaultValue)
                }

    }

    override var value: T
        get() = run {
            future.get()
            @Suppress("UNCHECKED_CAST")
            _value as T
        }
        set(value) = synchronized(this) {
            future.get()
            val newValue = value ?: default
            val update = newValue != _value
            _value = newValue
            if (update) {
                writeExecutor.submit {
                    preferences().edit().set(key, this.value).commit()
                }
                notifyChange()
            }
        }

    companion object {
        val readExecutor = Executors.newCachedThreadPool()
        private val writeExecutor = Executors.newSingleThreadExecutor()
    }

}

