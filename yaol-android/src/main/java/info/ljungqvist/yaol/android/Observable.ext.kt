package info.ljungqvist.yaol.android

import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.annotation.Keep
import androidx.databinding.*
import info.ljungqvist.yaol.Observable
import info.ljungqvist.yaol.Subscription
import info.ljungqvist.yaol.selfReference
import java.util.concurrent.CountDownLatch

private val handler by lazy { Handler(Looper.getMainLooper()) }
private fun <T> onMain(body: (T) -> Unit): (T) -> Unit = { value -> handler.post { body(value) } }

@CheckResult
fun <T> Observable<T>.onChangeOnMain(body: (T) -> Unit): Subscription =
        onChange(onMain(body))

/**
 * Subscribes to the observable. The supplied body will be posted on the android UI thread with the
 * current value of the observable once and then every time the value changes.
 *
 * @param postFirstRunOnMain if the true first execution of [body] will be synchronously
 * @param body the function to be executed
 * @return a subscription for unsubscribing
 */
@CheckResult
fun <T> Observable<T>.runAndOnChangeOnMain(postFirstRunOnMain: Boolean = true, body: (T) -> Unit): Subscription =
        onMain(body).let { bodyOnMain ->
            runAndOnChange(
                    when (postFirstRunOnMain) {
                        true -> bodyOnMain
                        false -> body
                    },
                    bodyOnMain
            )
        }

@CheckResult
fun <T> Observable<T>.onChangeUntilTrueOnMain(body: (T) -> Boolean): Subscription =
        selfReference {
            onChange(onMain {
                if (body(it)) {
                    self.close()
                }
            })
        }

@CheckResult
fun <T> Observable<T>.runAndOnChangeUntilTrueOnMain(body: (T) -> Boolean): Subscription {
    val latch = CountDownLatch(1)
    var ready = false
    val subscription = selfReference<Subscription> {
        onChange {
            latch.await()
            handler.post {
                if (ready || body(it)) {
                    self.close()
                }
            }
        }
    }
    handler.post {
        ready = body(value)
    }
    latch.countDown()
    return subscription
}

@Suppress("unused")
private class ReferenceHoldingOnPropertyChangedCallback(@Keep private vararg val hardReferences: Any)
    : androidx.databinding.Observable.OnPropertyChangedCallback() {
    override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) = Unit
}

private inline fun <T, O : BaseObservable> Observable<T>.databindingObservable(
        constructor: (T) -> O,
        crossinline set: O.(T) -> Unit
): O = constructor(value).also { observable ->
    val subscription = onChange { observable.set(it) }
    observable.addOnPropertyChangedCallback(ReferenceHoldingOnPropertyChangedCallback(subscription))
}

fun <T> Observable<T>.observableField(): ObservableField<T> =
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
