package com.example.therepairmate.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lang = LanguagePrefs.get(newBase)
        val locale = Locale.forLanguageTag(lang)
        val configuration = newBase.resources.configuration
        configuration.setLocale(locale)
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}
