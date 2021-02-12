package info.ljungqvist.yaol.testapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import info.ljungqvist.yaol.android.observableField
import info.ljungqvist.yaol.android.preferences.createObservablePreferenceFactory
import info.ljungqvist.yaol.lazyMutableWrapper
import info.ljungqvist.yaol.testapp.databinding.ActivityMainBinding
import info.ljungqvist.yaol.toData

class MainActivity : AppCompatActivity() {

    private val prefHolder by lazy { PrefHolder(this) }
    private var testProperty by lazyMutableWrapper { prefHolder.testProperty }
    private var testProperty2 by lazyMutableWrapper { prefHolder.testProperty2 }
    private var combo by lazyMutableWrapper { prefHolder.combo }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.data = Data(
                prefHolder.testProperty.observableField(),
                prefHolder.testProperty.join(prefHolder.testProperty2, prefHolder.combo) { text1, text2, (textcombo1, textcombo2) ->
                    "$text1, $text2, ($textcombo1, $textcombo2)"
                }.observableField()
        )

        binding.button1.setOnClickListener {
            testProperty = "text 1"
        }
        binding.button2.setOnClickListener {
            testProperty2 = "text 2"
        }
        binding.button3.setOnClickListener {
            combo = "first text 3" to "second text 3"
        }

    }

    data class Data(
            val value: ObservableField<String>,
            val value2: ObservableField<String>
    )
}

private class PrefHolder(context: Context) {
    private val prefFactory = context.createObservablePreferenceFactory("TEST")

    val testProperty = prefFactory.stringPreference("property") { "" }
    val testProperty2 = prefFactory.stringOptPreference("property2")
    val combo = testProperty.twoWayJoin(
            testProperty2,
            ::Pair
    ) { _, pair -> pair.toData() }
}
