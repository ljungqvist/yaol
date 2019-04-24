package info.ljungqvist.yaol.android.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import info.ljungqvist.yaol.MutableObservable
import java.util.concurrent.Future


interface ObservablePreferenceFactory {

    fun stringPreference(key: String, default: (SharedPreferences) -> String = { "" }): MutableObservable<String>

    fun stringOptPreference(key: String, default: (SharedPreferences) -> String? = { null }): MutableObservable<String?>

    fun stringSetPreference(key: String, default: (SharedPreferences) -> Set<String> = { emptySet() }): MutableObservable<Set<String>>

    fun intPreference(key: String, default: (SharedPreferences) -> Int = { 0 }): MutableObservable<Int>

    fun longPreference(key: String, default: (SharedPreferences) -> Long = { 0 }): MutableObservable<Long>

    fun floatPreference(key: String, default: (SharedPreferences) -> Float = { 0f }): MutableObservable<Float>

    fun booleanPreference(key: String, default: (SharedPreferences) -> Boolean = { false }): MutableObservable<Boolean>

    fun <T> migrate(body: (SharedPreferences) -> T): Future<T>

    companion object {

        fun create(context: Context, name: String): ObservablePreferenceFactory =
                ObservablePreferenceFactoryImpl(context, name)

        fun createWithDefaultSharedPreferencesName(context: Context): ObservablePreferenceFactory =
                ObservablePreferenceFactoryImpl(context, context.defaultSharedPreferencesName)

    }

}

private val Context.defaultSharedPreferencesName: String
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PreferenceManager.getDefaultSharedPreferencesName(this)
        } else {
            packageName + "_preferences"
        }
