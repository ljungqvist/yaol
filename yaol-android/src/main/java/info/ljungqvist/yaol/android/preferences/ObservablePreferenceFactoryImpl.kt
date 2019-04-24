package info.ljungqvist.yaol.android.preferences

import android.content.Context
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future


internal class ObservablePreferenceFactoryImpl(private val context: Context, private val name: String) :
        ObservablePreferenceFactory {

    private var preferences: SharedPreferences? = null

    private fun getPreferences(): SharedPreferences = synchronized(this) {
        preferences
                ?: context.getSharedPreferences(name, Context.MODE_PRIVATE)
                        .also { preferences = it }
    }

    private fun <T> preference(
            observables: MutableMap<Pair<String, String>, ObservablePreference<T>>,
            get: SharedPreferences.(String, T) -> T,
            set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
            key: String,
            default: (SharedPreferences) -> T
    ): MutableObservable<T> = synchronized(observables) {
        observables[name to key]
                ?: ObservablePreference(::getPreferences, key, get, set, default)
                        .also { observables[name to key] = it }
    }

    override fun stringPreference(key: String, default: (SharedPreferences) -> String): MutableObservable<String> =
            preference(stringObservables, SharedPreferences::getString, SharedPreferences.Editor::putString, key, default)

    override fun stringOptPreference(key: String, default: (SharedPreferences) -> String?): MutableObservable<String?> =
            preference(
                    stringOptObservables,
                    SharedPreferences::getString,
                    SharedPreferences.Editor::putString,
                    key,
                    default
            )

    override fun stringSetPreference(key: String, default: (SharedPreferences) -> Set<String>): MutableObservable<Set<String>> =
            preference(
                    stringSetObservables,
                    SharedPreferences::getStringSet,
                    SharedPreferences.Editor::putStringSet,
                    key,
                    default
            )

    override fun intPreference(key: String, default: (SharedPreferences) -> Int): MutableObservable<Int> =
            preference(intObservables, SharedPreferences::getInt, SharedPreferences.Editor::putInt, key, default)

    override fun longPreference(key: String, default: (SharedPreferences) -> Long): MutableObservable<Long> =
            preference(longObservables, SharedPreferences::getLong, SharedPreferences.Editor::putLong, key, default)

    override fun floatPreference(key: String, default: (SharedPreferences) -> Float): MutableObservable<Float> =
            preference(floatObservables, SharedPreferences::getFloat, SharedPreferences.Editor::putFloat, key, default)

    override fun booleanPreference(key: String, default: (SharedPreferences) -> Boolean): MutableObservable<Boolean> =
            preference(
                    booleanObservables,
                    SharedPreferences::getBoolean,
                    SharedPreferences.Editor::putBoolean,
                    key,
                    default
            )

    override fun <T> migrate(body: (SharedPreferences) -> T): Future<T> =
            ObservablePreference.readExecutor.submit(Callable {
                body(getPreferences())
            })

}

private fun <T> observables() = Collections.synchronizedMap(HashMap<Pair<String, String>, ObservablePreference<T>>())
private val stringObservables = observables<String>()
private val stringOptObservables = observables<String?>()
private val stringSetObservables = observables<Set<String>>()
private val intObservables = observables<Int>()
private val longObservables = observables<Long>()
private val floatObservables = observables<Float>()
private val booleanObservables = observables<Boolean>()
