package info.ljungqvist.yaol.android.preferences

import android.content.Context
import android.content.SharedPreferences
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

        @Deprecated(message = "use Context.createObservablePreferenceFactory(name)", replaceWith = ReplaceWith("context.createObservablePreferenceFactory(name)"))
        fun create(context: Context, name: String) =
                context.createObservablePreferenceFactory(name)

        @Deprecated("use Context.createObservablePreferenceFactory(name)", replaceWith = ReplaceWith("context.createObservablePreferenceFactory(context.packageName + \"_preferences\")"))
        fun createWithDefaultSharedPreferencesName(context: Context) =
                context.createObservablePreferenceFactory(context.packageName + "_preferences")

    }

}

fun Context.createObservablePreferenceFactory(name: String): ObservablePreferenceFactory =
        ObservablePreferenceFactoryImpl(this, name)
