package info.ljungqvist.yaol.testapp

import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import info.ljungqvist.yaol.android.PreferenceHolder
import info.ljungqvist.yaol.android.observableField
import info.ljungqvist.yaol.mutableObservableProperty
import info.ljungqvist.yaol.testapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val prefHolder by lazy { PrefHolder(this) }
    private var testProperty by mutableObservableProperty { prefHolder.testProperty }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.data = Data(prefHolder.testProperty.observableField())

        binding.button1.setOnClickListener { testProperty = "text 1" }
        binding.button2.setOnClickListener { testProperty = "text 2" }
        binding.button3.setOnClickListener { testProperty = "text 3" }


    }

    data class Data(val value: ObservableField<String>)
}

private class PrefHolder(context: Context) : PreferenceHolder(context, "TEST") {
    val testProperty = stringPreference("testProperty", "")
}
