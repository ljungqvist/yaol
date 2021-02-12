package info.ljungqvist.yaol.android

import androidx.annotation.Keep
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import info.ljungqvist.yaol.selfReference
import java.io.Closeable

fun Closeable.closeOnDestroy(lifecycle: Lifecycle): LifecycleObserver =
        selfReference {
            object : LifecycleObserver {
                @Keep
                @Suppress("unused")
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    close()
                    lifecycle.removeObserver(self)
                }
            }.also(lifecycle::addObserver)
        }

fun Closeable.closeOnDestroy(lifecycleOwner: LifecycleOwner): LifecycleObserver =
    closeOnDestroy(lifecycleOwner.lifecycle)
