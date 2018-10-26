# yaol
Yet Another Observable Library

## Usage

### yaol

```kotlin
class Store : Closeable {

    private val applesObservable = mutableObservable(0)
    /**
     * number of apples
     */
    var apples by mutableObservableProperty { applesObservable }

    private val applePriceObservable = mutableObservable(0.0)
    /**
     * price of apples
     */
    var applePrice by mutableObservableProperty { applePriceObservable }

    // join number of apples and price into one observable
    private val totalPriceObservable =
        applesObservable.join(applePriceObservable) { apples, price ->
            apples * price
        }
    /**
     * total price
     */
    val totalPrice by observableProperty { totalPriceObservable }

    // print the total price every time the number of apples or the price changes
    private val subscription =
        totalPriceObservable.runAndOnChange { totalPrice ->
            println("The total price it now $totalPrice!")
        }

    /**
     * unsubscribe to avoid memory leaks
     */
    override fun close() {
        subscription.unsubscribe()
    }

}
```

### yaol-android

```kotlin
// Add some sample code here
```

## Build

### Gradle

Include <https://dl.bintray.com/ljungqvist/yaol/> amongst your repositories:

```
repositories {
    maven { url "https://dl.bintray.com/ljungqvist/yaol" }
}
```

And the library amongst your dependencies:

```
dependencies {
    implementation 'info.ljungqvist:yaol:0.xx'
    // or
    implementation 'info.ljungqvist:yaol-android:0.xx'
}
```

### Deploy local

```
./gradlew clean yaol:build yaol:uploadArchives yaol-android:build yaol-android:uploadArchives
```

### Deploy to Bintray

```
./gradlew clean yaol:build yaol:uploadArchives yaol-android:build bintrayUpload
```

