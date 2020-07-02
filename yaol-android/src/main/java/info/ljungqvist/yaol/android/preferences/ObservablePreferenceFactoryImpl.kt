package info.ljungqvist.yaol.android.preferences

import android.content.Context
import android.content.SharedPreferences
import info.ljungqvist.yaol.MutableObservable
import java.lang.ref.WeakReference
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
            observables: ObservablesMap<T>,
            get: SharedPreferences.(String, T) -> T,
            set: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
            key: String,
            default: (SharedPreferences) -> T
    ): MutableObservable<T> = synchronized(observables) {
        observables[name to key]
                ?.get()
                ?: ObservablePreference(::getPreferences, key, get, set, default)
                        .also { observables[name to key] = WeakReference(it) }
    }

    override fun stringPreference(key: String, default: (SharedPreferences) -> String): MutableObservable<String> =
            preference(
                    stringObservables,
                    { k, d -> getString(k, d) ?: d },
                    SharedPreferences.Editor::putString,
                    key,
                    default
            )

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
                    { k, d -> getStringSet(k, d) ?: d },
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

private typealias ObservablesMap<T> = MutableMap<Pair<String, String>, WeakReference<ObservablePreference<T>>>

private fun <T> observables(): ObservablesMap<T> =
        Collections.synchronizedMap(HashMap<Pair<String, String>, WeakReference<ObservablePreference<T>>>())

private val stringObservables: ObservablesMap<String> = observables()
private val stringOptObservables: ObservablesMap<String?> = observables()
private val stringSetObservables: ObservablesMap<Set<String>> = observables()
private val intObservables: ObservablesMap<Int> = observables()
private val longObservables: ObservablesMap<Long> = observables()
private val floatObservables: ObservablesMap<Float> = observables()
private val booleanObservables: ObservablesMap<Boolean> = observables()
