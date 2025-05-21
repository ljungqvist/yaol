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
    var apples by applesObservable

    private val applePriceObservable = mutableObservable(0.0)
    /**
     * price of apples
     */
    var applePrice by applePriceObservable

    // join number of apples and price into one observable
    private val totalPriceObservable =
        applesObservable.join(applePriceObservable) { apples, price ->
            apples * price
        }
    /**
     * total price
     */
    val totalPrice by totalPriceObservable

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

