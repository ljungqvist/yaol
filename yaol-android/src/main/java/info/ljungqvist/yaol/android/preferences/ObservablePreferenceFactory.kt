package info.ljungqvist.yaol.android.preferences

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import info.ljungqvist.yaol.MutableObservable


interface ObservablePreferenceFactory {

    fun stringPreference(key: String, default: String = ""): MutableObservable<String>

    fun stringOptPreference(key: String, default: String? = null): MutableObservable<String?>

    fun stringSetPreference(key: String, default: Set<String> = emptySet()): MutableObservable<Set<String>>

    fun intPreference(key: String, default: Int = 0): MutableObservable<Int>

    fun longPreference(key: String, default: Long = 0): MutableObservable<Long>

    fun floatPreference(key: String, default: Float = 0f): MutableObservable<Float>

    fun booleanPreference(key: String, default: Boolean = false): MutableObservable<Boolean>

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
