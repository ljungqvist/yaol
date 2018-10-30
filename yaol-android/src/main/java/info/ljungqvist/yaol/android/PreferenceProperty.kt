package info.ljungqvist.yaol.android

import android.content.Context
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import info.ljungqvist.yaol.MutableObservableImpl
import info.ljungqvist.yaol.ObservableImpl
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private fun <T> observables() = Collections.synchronizedMap(HashMap<Pair<String, String>, WeakReference<PreferenceObservable<T>>>())
private val stringObservables = observables<String>()
private val stringOptObservables = observables<String?>()
private val stringSetObservables = observables<Set<String>>()
private val intObservables = observables<Int>()
private val longObservables = observables<Long>()
private val floatObservables = observables<Float>()
private val booleanObservables = observables<Boolean>()

abstract class PreferenceHolder(context: Context, private val name: String) {
    private val preferences: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    private fun <T> preference(
            observables: MutableMap<Pair<String, String>, WeakReference<PreferenceObservable<T>>>,
            get: SharedPreferences.(String, T) -> T,
            set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
            key: String,
            default: T
    ): MutableObservable<T> = synchronized(observables) {
        observables[name to key]?.get()
                ?: PreferenceObservable(executor, preferences, key, get, set, default)
                        .also { observables[name to key] = WeakReference(it) }
    }

    protected fun stringPreference(key: String, default: String = ""): MutableObservable<String> =
            preference(stringObservables, SharedPreferences::getString, SharedPreferences.Editor::putString, key, default)

    protected fun stringOptPreference(key: String, default: String? = null): MutableObservable<String?> =
            preference(stringOptObservables, SharedPreferences::getString, SharedPreferences.Editor::putString, key, default)

    protected fun stringSetPreference(key: String, default: Set<String> = emptySet()): MutableObservable<Set<String>> =
            preference(stringSetObservables, SharedPreferences::getStringSet, SharedPreferences.Editor::putStringSet, key, default)

    protected fun intPreference(key: String, default: Int = 0): MutableObservable<Int> =
            preference(intObservables, SharedPreferences::getInt, SharedPreferences.Editor::putInt, key, default)

    protected fun longPreference(key: String, default: Long = 0): MutableObservable<Long> =
            preference(longObservables, SharedPreferences::getLong, SharedPreferences.Editor::putLong, key, default)

    protected fun floatPreference(key: String, default: Float = 0f): MutableObservable<Float> =
            preference(floatObservables, SharedPreferences::getFloat, SharedPreferences.Editor::putFloat, key, default)

    protected fun booleanPreference(key: String, default: Boolean = false): MutableObservable<Boolean> =
            preference(booleanObservables, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean, key, default)

}

private class PreferenceObservable<T>(
        private val executor: ExecutorService,
        private val preferences: SharedPreferences,
        private val key: String,
        get: SharedPreferences.(String, T) -> T,
        private val set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
        default: T
) : ObservableImpl<T>(), MutableObservable<T> {
    private val latch = CountDownLatch(1)
    private var ready = false

    private var _value: T? = null

    override var value: T
        get() = run {
            if (!ready) latch.await()
            @Suppress("UNCHECKED_CAST")
            _value as T
        }
        set(value) = synchronized(this) {
            val update = !ready || value != _value
            _value = value
            ready = true
            latch.countDown()
            if (update) {
                notifyChange()
                executor.submit {
                    preferences.edit().set(key, value).apply()
                }
            }
        }

    init {
        executor.submit {
            value = preferences.get(key, default)
        }
    }

}

