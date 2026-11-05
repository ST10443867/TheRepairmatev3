package com.example.therepairmate.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.therepairmate.R
import com.example.therepairmate.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/** Professional spring press effect on touch, so cards and buttons feel highly tactile. */
@SuppressLint("ClickableViewAccessibility")
fun View.pressEffect() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(3f)).setDuration(280).start()
        }
        false
    }
}

fun AppCompatActivity.confirmSignOut() {
    val activity = this
    val options = arrayOf("Change Language", "Sign out")
    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle("Account (${Repo.email ?: "User"})")
        .setItems(options) { _, which ->
            when (which) {
                0 -> showLanguageDialog(activity)
                1 -> {
                    Repo.signOut()
                    val app = applicationContext
                    startActivity(
                        Intent(this, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finishAffinity()
                    CoroutineScope(Dispatchers.Main).launch {
                        try { CredentialManager.create(app).clearCredentialState(ClearCredentialStateRequest()) }
                        catch (e: Exception) { }
                    }
                }
            }
        }
        .setNegativeButton("Close", null)
        .show()
}

fun AppCompatActivity.showLanguageDialog(activity: AppCompatActivity) {
    val languages = arrayOf("English", "Afrikaans", "Setswana", "isiXhosa", "isiZulu")
    val tags = arrayOf("en", "af", "tn", "xh", "zu")
    val currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val currentIndex = tags.indexOfFirst { currentTag.startsWith(it) }.let { if (it < 0) 0 else it }

    com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
        .setTitle("Choose Language / Tlhopha Puo / Khetha Ulwimi")
        .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
            val selectedLang = tags[which]
            LanguagePrefs.set(activity, selectedLang)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(selectedLang)
            )
            CoroutineScope(Dispatchers.IO).launch {
                try { Repo.updateLanguage(selectedLang) } catch (e: Exception) { }
            }
            toast("Language updated")
            dialog.dismiss()
            activity.recreate()
        }
        .setNegativeButton("Cancel", null)
        .show()
}

object LanguagePrefs {
    private const val PREF = "repairmate_lang_pref"
    private const val KEY_LANG = "language"

    fun get(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LANG, "en") ?: "en"
    }

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_LANG, lang).apply()
    }
}

fun AppCompatActivity.applyLocalLanguage() {
    val lang = LanguagePrefs.get(this)
    val current = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
    if (!current.contains(lang)) {
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(lang)
        )
    }
}

fun AppCompatActivity.addBackBar() {
    val content = findViewById<ViewGroup>(android.R.id.content)
    val original = content.getChildAt(0) ?: return
    content.removeView(original)
    val d = resources.displayMetrics.density
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)

    val back = ImageButton(this).apply {
        setImageResource(R.drawable.ic_back)
        setBackgroundResource(tv.resourceId)
        contentDescription = "Back"
        setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
    val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    column.addView(back, LinearLayout.LayoutParams((48 * d).toInt(), (48 * d).toInt()).apply {
        leftMargin = (8 * d).toInt(); topMargin = (4 * d).toInt()
    })
    column.addView(original, LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    content.addView(column, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}

/** Smooth scroll RecyclerView to top */
fun androidx.recyclerview.widget.RecyclerView.smoothScrollToTop() {
    if ((layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0 > 5) {
        scrollToPosition(5)
    }
    smoothScrollToPosition(0)
}

/** Smooth scroll ScrollView to top */
fun android.widget.ScrollView.smoothScrollToTop() {
    smoothScrollTo(0, 0)
}

fun android.widget.ImageView.loadUrl(url: String?) {
    if (url.isNullOrBlank()) {
        setImageResource(R.drawable.ic_phone)
        return
    }
    setImageDrawable(null)
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val stream = java.net.URL(url).openStream()
            val bmp = android.graphics.BitmapFactory.decodeStream(stream)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                setImageBitmap(bmp)
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                setImageResource(R.drawable.ic_phone)
            }
        }
    }
}
