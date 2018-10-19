package info.ljungqvist.yaol.android

import android.databinding.*
import android.os.Handler
import android.os.Looper
import info.ljungqvist.yaol.Observable
import info.ljungqvist.yaol.Subscription
import info.ljungqvist.yaol.selfReference

private val handler by lazy { Handler(Looper.getMainLooper()) }
private fun <T> onMain(body: (T) -> Unit): (T) -> Unit = { value -> handler.post { body(value) } }

fun <T> Observable<T>.onChangeOnMain(body: (T) -> Unit): Subscription =
    onChange(onMain(body))

fun <T> Observable<T>.runAndOnChangeOnMain(body: (T) -> Unit): Subscription {
    body(value)
    return onChangeOnMain(body)
}

fun <T> Observable<T>.runAndOnChangeUntilTrueOnMain(body: (T) -> Boolean) {
    if (!body(value)) {
        selfReference<Subscription> {
            onChangeOnMain {
                if (body(it)) {
                    self.unsubscribe()
                }
            }
        }
    }
}

fun <T> Observable<T>.observableField(): android.databinding.ObservableField<T> =
    ObservableField(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Boolean>.primitive(): ObservableBoolean =
    ObservableBoolean(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Byte>.primitive(): ObservableByte =
    ObservableByte(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Char>.primitive(): ObservableChar =
    ObservableChar(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Short>.primitive(): ObservableShort =
    ObservableShort(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Int>.primitive(): ObservableInt =
    ObservableInt(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Long>.primitive(): ObservableLong =
    ObservableLong(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Float>.primitive(): ObservableFloat =
    ObservableFloat(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }

fun Observable<Double>.primitive(): ObservableDouble =
    ObservableDouble(value).also { observable ->
        onChangeWeak { observable.set(it) }
    }
