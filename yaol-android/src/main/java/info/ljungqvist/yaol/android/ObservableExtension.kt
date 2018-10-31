package info.ljungqvist.yaol.android

import android.os.Handler
import android.os.Looper
import androidx.databinding.*
import info.ljungqvist.yaol.Observable
import info.ljungqvist.yaol.Subscription
import info.ljungqvist.yaol.selfReference
import java.lang.ref.WeakReference

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

private inline fun <T, O : BaseObservable> Observable<T>.databindingObservable(
    constructor: (T) -> O,
    crossinline set: O.(T) -> Unit
): O =
    constructor(value)
        .also { observable ->
            val ref = WeakReference(observable)
            onChangeUntilTrue {
                ref.get()?.set(it) == null
            }
        }

fun <T> Observable<T>.observableField(): androidx.databinding.ObservableField<T> =
    databindingObservable(::ObservableField) { set(it) }

fun Observable<Boolean>.primitive(): ObservableBoolean =
    databindingObservable(::ObservableBoolean) { set(it) }

fun Observable<Byte>.primitive(): ObservableByte =
    databindingObservable(::ObservableByte) { set(it) }

fun Observable<Char>.primitive(): ObservableChar =
    databindingObservable(::ObservableChar) { set(it) }

fun Observable<Short>.primitive(): ObservableShort =
    databindingObservable(::ObservableShort) { set(it) }

fun Observable<Int>.primitive(): ObservableInt =
    databindingObservable(::ObservableInt) { set(it) }

fun Observable<Long>.primitive(): ObservableLong =
    databindingObservable(::ObservableLong) { set(it) }

fun Observable<Float>.primitive(): ObservableFloat =
    databindingObservable(::ObservableFloat) { set(it) }

fun Observable<Double>.primitive(): ObservableDouble =
    databindingObservable(::ObservableDouble) { set(it) }
