package info.ljungqvist.yaol.android.preferences

import android.content.Context
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import java.lang.ref.WeakReference
import java.util.*


internal class ObservablePreferenceFactoryImpl(context: Context, private val name: String) : ObservablePreferenceFactory {
    private val preferences: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun <T> preference(
            observables: MutableMap<Pair<String, String>, WeakReference<ObservablePreference<T>>>,
            get: SharedPreferences.(String, T) -> T,
            set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
            key: String,
            default: T
    ): MutableObservable<T> = synchronized(observables) {
        observables[name to key]?.get()
                ?: ObservablePreference(preferences, key, get, set, default)
                        .also { observables[name to key] = WeakReference(it) }
    }

    override fun stringPreference(key: String, default: String): MutableObservable<String> =
            preference(stringObservables, SharedPreferences::getString, SharedPreferences.Editor::putString, key, default)

    override fun stringOptPreference(key: String, default: String?): MutableObservable<String?> =
            preference(stringOptObservables, SharedPreferences::getString, SharedPreferences.Editor::putString, key, default)

    override fun stringSetPreference(key: String, default: Set<String>): MutableObservable<Set<String>> =
            preference(stringSetObservables, SharedPreferences::getStringSet, SharedPreferences.Editor::putStringSet, key, default)

    override fun intPreference(key: String, default: Int): MutableObservable<Int> =
            preference(intObservables, SharedPreferences::getInt, SharedPreferences.Editor::putInt, key, default)

    override fun longPreference(key: String, default: Long): MutableObservable<Long> =
            preference(longObservables, SharedPreferences::getLong, SharedPreferences.Editor::putLong, key, default)

    override fun floatPreference(key: String, default: Float): MutableObservable<Float> =
            preference(floatObservables, SharedPreferences::getFloat, SharedPreferences.Editor::putFloat, key, default)

    override fun booleanPreference(key: String, default: Boolean): MutableObservable<Boolean> =
            preference(booleanObservables, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean, key, default)

}

private fun <T> observables() = Collections.synchronizedMap(HashMap<Pair<String, String>, WeakReference<ObservablePreference<T>>>())
private val stringObservables = observables<String>()
private val stringOptObservables = observables<String?>()
private val stringSetObservables = observables<Set<String>>()
private val intObservables = observables<Int>()
private val longObservables = observables<Long>()
private val floatObservables = observables<Float>()
private val booleanObservables = observables<Boolean>()
