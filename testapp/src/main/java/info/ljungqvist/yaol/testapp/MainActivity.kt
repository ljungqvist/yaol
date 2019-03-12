package info.ljungqvist.yaol.testapp

import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import info.ljungqvist.yaol.android.observableField
import info.ljungqvist.yaol.android.preferences.ObservablePreferenceFactory
import info.ljungqvist.yaol.lazyMutableWrapper
import info.ljungqvist.yaol.testapp.databinding.ActivityMainBinding
import info.ljungqvist.yaol.toData

class MainActivity : AppCompatActivity() {

    private val prefHolder by lazy { PrefHolder(this) }
    private var testProperty by lazyMutableWrapper { prefHolder.testProperty }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.data = Data(
            prefHolder.testProperty.observableField(),
            prefHolder.testProperty.join(prefHolder.combo) { text1, (text2, text3) ->
                "$text1, $text2, $text3"
            }.observableField()
        )

        binding.button1.setOnClickListener { testProperty = "text 1" }
        binding.button2.setOnClickListener { testProperty = "text 2" }
        binding.button3.setOnClickListener { testProperty = "text 3" }

    }

    data class Data(
        val value: ObservableField<String>,
        val value2: ObservableField<String>
    )
}

private class PrefHolder(context: Context) {
    private val prefFactory = ObservablePreferenceFactory.create(context, "TEST")

    val testProperty = prefFactory.stringPreference("property", "")
    val testProperty2 = prefFactory.stringOptPreference("property2")
    val combo = testProperty.twoWayJoin(
        testProperty2,
        ::Pair
    ) { it.toData() }
}
